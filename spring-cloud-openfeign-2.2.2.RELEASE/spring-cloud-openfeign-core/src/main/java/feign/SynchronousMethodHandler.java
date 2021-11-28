/**
 * Copyright 2012-2019 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package feign;

import static feign.ExceptionPropagationPolicy.UNWRAP;
import static feign.FeignException.errorExecuting;
import static feign.FeignException.errorReading;
import static feign.Util.checkNotNull;
import static feign.Util.ensureClosed;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Request.Options;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

final class SynchronousMethodHandler implements MethodHandler {

	private static final long MAX_RESPONSE_BUFFER_SIZE = 8192L;

	private final MethodMetadata metadata;
	private final Target<?> target;
	private final Client client;
	private final Retryer retryer;
	private final List<RequestInterceptor> requestInterceptors;
	private final Logger logger;
	private final Logger.Level logLevel;
	private final RequestTemplate.Factory buildTemplateFromArgs;
	private final Options options;
	private final Decoder decoder;
	private final ErrorDecoder errorDecoder;
	private final boolean decode404;
	private final boolean closeAfterDecode;
	private final ExceptionPropagationPolicy propagationPolicy;

	private SynchronousMethodHandler(Target<?> target, Client client, Retryer retryer,
		List<RequestInterceptor> requestInterceptors, Logger logger,
		Logger.Level logLevel, MethodMetadata metadata,
		RequestTemplate.Factory buildTemplateFromArgs, Options options,
		Decoder decoder, ErrorDecoder errorDecoder, boolean decode404,
		boolean closeAfterDecode, ExceptionPropagationPolicy propagationPolicy) {
		this.target = checkNotNull(target, "target");
		this.client = checkNotNull(client, "client for %s", target);
		this.retryer = checkNotNull(retryer, "retryer for %s", target);
		this.requestInterceptors = checkNotNull(requestInterceptors, "requestInterceptors for %s", target);
		this.logger = checkNotNull(logger, "logger for %s", target);
		this.logLevel = checkNotNull(logLevel, "logLevel for %s", target);
		this.metadata = checkNotNull(metadata, "metadata for %s", target);
		this.buildTemplateFromArgs = checkNotNull(buildTemplateFromArgs, "metadata for %s", target);
		this.options = checkNotNull(options, "options for %s", target);
		this.errorDecoder = checkNotNull(errorDecoder, "errorDecoder for %s", target);
		this.decoder = checkNotNull(decoder, "decoder for %s", target);
		this.decode404 = decode404;
		this.closeAfterDecode = closeAfterDecode;
		this.propagationPolicy = propagationPolicy;
	}

	@Override
	public Object invoke(Object[] argv) throws Throwable {
		// 被 @RequestParam 和 @PathVariable 注解的参数最终会被拼接在url后面，如果是Object对象如何拼接？ 调用toString()方法然后urlEncode
		// 对于没有注解或者是被 @RequestBody 注解的参数，被写入到 RequestTemplate 的请求体body中
		RequestTemplate template = buildTemplateFromArgs.create(argv);

		// 方法参数中传递 Options 对象，这种用法没见过，可能是兼容老版本
		Options options = findOptions(argv);
		Retryer retryer = this.retryer.clone();
		while (true) {
			try {
				return executeAndDecode(template, options);
			} catch (RetryableException e) {
				try {
					retryer.continueOrPropagate(e);
				} catch (RetryableException th) {
					Throwable cause = th.getCause();
					if (propagationPolicy == UNWRAP && cause != null) {
						throw cause;
					} else {
						throw th;
					}
				}
				if (logLevel != Logger.Level.NONE) {
					logger.logRetry(metadata.configKey(), logLevel);
				}
				continue;
			}
		}
	}

	Object executeAndDecode(RequestTemplate template, Options options) throws Throwable {
		Request request = targetRequest(template);

		if (logLevel != Logger.Level.NONE) {
			logger.logRequest(metadata.configKey(), logLevel, request);
		}

		Response response;
		long start = System.nanoTime();
		try {
			response = client.execute(request, options);
			// ensure the request is set. TODO: remove in Feign 12
			response = response.toBuilder().request(request).requestTemplate(template).build();
		} catch (IOException e) {
			// 不知道什么情况下会抛出 IOException 异常
			// 比如读取超时，可以自己写demo模拟
			if (logLevel != Logger.Level.NONE) {
				logger.logIOException(metadata.configKey(), logLevel, e, elapsedTime(start));
			}
			// 抛出 RetryableException 异常
			throw errorExecuting(request, e);
		}
		long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
		boolean shouldClose = true;

		// 对于正常返回包括 404、500 等都会走下面
		try {
			if (logLevel != Logger.Level.NONE) {
				// todo 先注掉
				// response = logger.logAndRebufferResponse(metadata.configKey(), logLevel, response, elapsedTime);
			}
			// 方法返回值是response
			if (Response.class == metadata.returnType()) {
				if (response.body() == null) {
					return response;
				}
				if (response.body().length() == null || response.body().length() > MAX_RESPONSE_BUFFER_SIZE) {
					shouldClose = false;
					return response;
				}
				// Ensure the response body is disconnected
				byte[] bodyData = Util.toByteArray(response.body().asInputStream());
				return response.toBuilder().body(bodyData).build();
			}

			// 返回状态码正常
			if (response.status() >= 200 && response.status() < 300) {
				if (void.class == metadata.returnType()) {
					return null;
				} else {
					// 解码
					Object result = decode(response);
					shouldClose = closeAfterDecode;
					return result;
				}

			} else if (decode404 && response.status() == 404 && void.class != metadata.returnType()) {
				// 单独处理返回码 404 的情况
				Object result = decode(response);
				shouldClose = closeAfterDecode;
				return result;
			} else {
				throw errorDecoder.decode(metadata.configKey(), response);
			}
		} catch (IOException e) {
			if (logLevel != Logger.Level.NONE) {
				logger.logIOException(metadata.configKey(), logLevel, e, elapsedTime);
			}

			// 抛出 FeignException 异常
			throw errorReading(request, response, e);
		} finally {
			if (shouldClose) {
				ensureClosed(response.body());
			}
		}
	}

	long elapsedTime(long start) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
	}

	Request targetRequest(RequestTemplate template) {
		for (RequestInterceptor interceptor : requestInterceptors) {
			interceptor.apply(template);
		}
		return target.apply(template);
	}

	Object decode(Response response) throws Throwable {
		try {
			return decoder.decode(response, metadata.returnType());
		} catch (FeignException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new DecodeException(response.status(), e.getMessage(), response.request(), e);
		}
	}

	Options findOptions(Object[] argv) {
		if (argv == null || argv.length == 0) {
			return this.options;
		}
		return Stream.of(argv)
			.filter(Options.class::isInstance)
			.map(Options.class::cast)
			.findFirst()
			.orElse(this.options);
	}

	static class Factory {

		private final Client client;
		private final Retryer retryer;
		private final List<RequestInterceptor> requestInterceptors;
		private final Logger logger;
		private final Logger.Level logLevel;
		private final boolean decode404;
		private final boolean closeAfterDecode;
		private final ExceptionPropagationPolicy propagationPolicy;

		Factory(Client client, Retryer retryer, List<RequestInterceptor> requestInterceptors,
			Logger logger, Logger.Level logLevel, boolean decode404, boolean closeAfterDecode,
			ExceptionPropagationPolicy propagationPolicy) {
			this.client = checkNotNull(client, "client");
			this.retryer = checkNotNull(retryer, "retryer");
			this.requestInterceptors = checkNotNull(requestInterceptors, "requestInterceptors");
			this.logger = checkNotNull(logger, "logger");
			this.logLevel = checkNotNull(logLevel, "logLevel");
			this.decode404 = decode404;
			this.closeAfterDecode = closeAfterDecode;
			this.propagationPolicy = propagationPolicy;
		}

		public MethodHandler create(Target<?> target,
			MethodMetadata md,
			RequestTemplate.Factory buildTemplateFromArgs,
			Options options,
			Decoder decoder,
			ErrorDecoder errorDecoder) {
			return new SynchronousMethodHandler(target, client, retryer, requestInterceptors, logger,
				logLevel, md, buildTemplateFromArgs, options, decoder,
				errorDecoder, decode404, closeAfterDecode, propagationPolicy);
		}
	}
}
