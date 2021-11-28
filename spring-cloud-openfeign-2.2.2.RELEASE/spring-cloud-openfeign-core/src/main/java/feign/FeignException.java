/**
 * Copyright 2012-2020 The Feign Authors
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

import static feign.Util.UTF_8;
import static feign.Util.checkNotNull;
import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Origin exception type for all Http Apis.
 */
public class FeignException extends RuntimeException {

	private static final String EXCEPTION_MESSAGE_TEMPLATE_NULL_REQUEST = "request should not be null";
	private static final long serialVersionUID = 0;
	private int status;
	private byte[] responseBody;
	private Request request;

	protected FeignException(int status, String message, Throwable cause) {
		super(message, cause);
		this.status = status;
		this.request = null;
	}

	protected FeignException(int status, String message, Throwable cause, byte[] responseBody) {
		super(message, cause);
		this.status = status;
		this.responseBody = responseBody;
		this.request = null;
	}

	protected FeignException(int status, String message) {
		super(message);
		this.status = status;
		this.request = null;
	}

	protected FeignException(int status, String message, byte[] responseBody) {
		super(message);
		this.status = status;
		this.responseBody = responseBody;
		this.request = null;
	}

	protected FeignException(int status, String message, Request request, Throwable cause) {
		super(message, cause);
		this.status = status;
		this.request = checkRequestNotNull(request);
	}

	protected FeignException(int status, String message, Request request, Throwable cause,
		byte[] responseBody) {
		super(message, cause);
		this.status = status;
		this.responseBody = responseBody;
		this.request = checkRequestNotNull(request);
	}

	protected FeignException(int status, String message, Request request) {
		super(message);
		this.status = status;
		this.request = checkRequestNotNull(request);
	}

	protected FeignException(int status, String message, Request request, byte[] responseBody) {
		super(message);
		this.status = status;
		this.responseBody = responseBody;
		this.request = checkRequestNotNull(request);
	}

	private Request checkRequestNotNull(Request request) {
		return checkNotNull(request, EXCEPTION_MESSAGE_TEMPLATE_NULL_REQUEST);
	}

	public int status() {
		return this.status;
	}

	/**
	 * The Response Body, if present.
	 *
	 * @return the body of the response.
	 * @deprecated use {@link #responseBody()} instead.
	 */
	@Deprecated
	public byte[] content() {
		return this.responseBody;
	}

	/**
	 * The Response body.
	 *
	 * @return an Optional wrapping the response body.
	 */
	public Optional<ByteBuffer> responseBody() {
		if (this.responseBody == null) {
			return Optional.empty();
		}
		return Optional.of(ByteBuffer.wrap(this.responseBody));
	}

	public Request request() {
		return this.request;
	}

	public boolean hasRequest() {
		return (this.request != null);
	}

	public String contentUTF8() {
		if (responseBody != null) {
			return new String(responseBody, UTF_8);
		} else {
			return "";
		}
	}

	static FeignException errorReading(Request request, Response response, IOException cause) {
		return new FeignException(
			response.status(),
			format("%s reading %s %s", cause.getMessage(), request.httpMethod(), request.url()),
			request,
			cause,
			request.body());
	}

	public static FeignException errorStatus(String methodKey, Response response) {

		byte[] body = {};
		try {
			if (response.body() != null) {
				body = Util.toByteArray(response.body().asInputStream());
			}
		} catch (IOException ignored) { // NOPMD
		}

		String message = new FeignExceptionMessageBuilder()
			.withResponse(response)
			.withMethodKey(methodKey)
			.withBody(body).build();

		return errorStatus(response.status(), message, response.request(), body);
	}

	private static FeignException errorStatus(int status,
		String message,
		Request request,
		byte[] body) {
		if (isClientError(status)) {
			return clientErrorStatus(status, message, request, body);
		}
		if (isServerError(status)) {
			return serverErrorStatus(status, message, request, body);
		}
		return new FeignException(status, message, request, body);
	}

	private static boolean isClientError(int status) {
		return status >= 400 && status < 500;
	}

	private static FeignClientException clientErrorStatus(int status,
		String message,
		Request request,
		byte[] body) {
		switch (status) {
			case 400:
				return new BadRequest(message, request, body);
			case 401:
				return new Unauthorized(message, request, body);
			case 403:
				return new Forbidden(message, request, body);
			case 404:
				return new NotFound(message, request, body);
			case 405:
				return new MethodNotAllowed(message, request, body);
			case 406:
				return new NotAcceptable(message, request, body);
			case 409:
				return new Conflict(message, request, body);
			case 410:
				return new Gone(message, request, body);
			case 415:
				return new UnsupportedMediaType(message, request, body);
			case 429:
				return new TooManyRequests(message, request, body);
			case 422:
				return new UnprocessableEntity(message, request, body);
			default:
				return new FeignClientException(status, message, request, body);
		}
	}

	private static boolean isServerError(int status) {
		return status >= 500 && status <= 599;
	}

	private static FeignServerException serverErrorStatus(int status,
		String message,
		Request request,
		byte[] body) {
		switch (status) {
			case 500:
				return new InternalServerError(message, request, body);
			case 501:
				return new NotImplemented(message, request, body);
			case 502:
				return new BadGateway(message, request, body);
			case 503:
				return new ServiceUnavailable(message, request, body);
			case 504:
				return new GatewayTimeout(message, request, body);
			default:
				return new FeignServerException(status, message, request, body);
		}
	}

	static FeignException errorExecuting(Request request, IOException cause) {
		return new RetryableException(
			-1,
			format("%s executing %s %s", cause.getMessage(), request.httpMethod(), request.url()),
			request.httpMethod(),
			cause,
			null, request);
	}

	public static class FeignClientException extends FeignException {

		public FeignClientException(int status, String message, Request request, byte[] body) {
			super(status, message, request, body);
		}
	}


	public static class BadRequest extends FeignClientException {

		public BadRequest(String message, Request request, byte[] body) {
			super(400, message, request, body);
		}
	}


	public static class Unauthorized extends FeignClientException {

		public Unauthorized(String message, Request request, byte[] body) {
			super(401, message, request, body);
		}
	}


	public static class Forbidden extends FeignClientException {

		public Forbidden(String message, Request request, byte[] body) {
			super(403, message, request, body);
		}
	}


	public static class NotFound extends FeignClientException {

		public NotFound(String message, Request request, byte[] body) {
			super(404, message, request, body);
		}
	}


	public static class MethodNotAllowed extends FeignClientException {

		public MethodNotAllowed(String message, Request request, byte[] body) {
			super(405, message, request, body);
		}
	}


	public static class NotAcceptable extends FeignClientException {

		public NotAcceptable(String message, Request request, byte[] body) {
			super(406, message, request, body);
		}
	}


	public static class Conflict extends FeignClientException {

		public Conflict(String message, Request request, byte[] body) {
			super(409, message, request, body);
		}
	}


	public static class Gone extends FeignClientException {

		public Gone(String message, Request request, byte[] body) {
			super(410, message, request, body);
		}
	}


	public static class UnsupportedMediaType extends FeignClientException {

		public UnsupportedMediaType(String message, Request request, byte[] body) {
			super(415, message, request, body);
		}
	}


	public static class TooManyRequests extends FeignClientException {

		public TooManyRequests(String message, Request request, byte[] body) {
			super(429, message, request, body);
		}
	}


	public static class UnprocessableEntity extends FeignClientException {

		public UnprocessableEntity(String message, Request request, byte[] body) {
			super(422, message, request, body);
		}
	}


	public static class FeignServerException extends FeignException {

		public FeignServerException(int status, String message, Request request, byte[] body) {
			super(status, message, request, body);
		}
	}


	public static class InternalServerError extends FeignServerException {

		public InternalServerError(String message, Request request, byte[] body) {
			super(500, message, request, body);
		}
	}


	public static class NotImplemented extends FeignServerException {

		public NotImplemented(String message, Request request, byte[] body) {
			super(501, message, request, body);
		}
	}


	public static class BadGateway extends FeignServerException {

		public BadGateway(String message, Request request, byte[] body) {
			super(502, message, request, body);
		}
	}


	public static class ServiceUnavailable extends FeignServerException {

		public ServiceUnavailable(String message, Request request, byte[] body) {
			super(503, message, request, body);
		}
	}


	public static class GatewayTimeout extends FeignServerException {

		public GatewayTimeout(String message, Request request, byte[] body) {
			super(504, message, request, body);
		}
	}


	private static class FeignExceptionMessageBuilder {

		private static final int MAX_BODY_BYTES_LENGTH = 400;
		private static final int MAX_BODY_CHARS_LENGTH = 200;

		private Response response;

		private byte[] body;
		private String methodKey;

		public FeignExceptionMessageBuilder withResponse(Response response) {
			this.response = response;
			return this;
		}

		public FeignExceptionMessageBuilder withBody(byte[] body) {
			this.body = body;
			return this;
		}

		public FeignExceptionMessageBuilder withMethodKey(String methodKey) {
			this.methodKey = methodKey;
			return this;
		}

		public String build() {
			StringBuilder result = new StringBuilder();

			if (response.reason() != null) {
				result.append(format("[%d %s]", response.status(), response.reason()));
			} else {
				result.append(format("[%d]", response.status()));
			}
			result.append(format(" during [%s] to [%s] [%s]", response.request().httpMethod(), response.request().url(), methodKey));

			result.append(format(": [%s]", getBodyAsString(body, response.headers())));

			return result.toString();
		}

		private static String getBodyAsString(byte[] body, Map<String, Collection<String>> headers) {
			Charset charset = getResponseCharset(headers);
			if (charset == null) {
				charset = Util.UTF_8;
			}
			return getResponseBody(body, charset);
		}

		private static String getResponseBody(byte[] body, Charset charset) {
			if (body.length < MAX_BODY_BYTES_LENGTH) {
				return new String(body, charset);
			}
			return getResponseBodyPreview(body, charset);
		}

		private static String getResponseBodyPreview(byte[] body, Charset charset) {
			try {
				Reader reader = new InputStreamReader(new ByteArrayInputStream(body), charset);
				CharBuffer result = CharBuffer.allocate(MAX_BODY_CHARS_LENGTH);

				reader.read(result);
				reader.close();
				((Buffer) result).flip();
				return result.toString() + "... (" + body.length + " bytes)";
			} catch (IOException e) {
				return e.toString() + ", failed to parse response";
			}
		}

		private static Charset getResponseCharset(Map<String, Collection<String>> headers) {

			Collection<String> strings = headers.get("content-type");
			if (strings == null || strings.size() == 0) {
				return null;
			}

			Pattern pattern = Pattern.compile("charset=([^\\s])");
			Matcher matcher = pattern.matcher(strings.iterator().next());
			if (!matcher.lookingAt()) {
				return null;
			}

			String group = matcher.group(1);
			if (!Charset.isSupported(group)) {
				return null;
			}
			return Charset.forName(group);

		}
	}
}
