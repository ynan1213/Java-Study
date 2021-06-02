/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FileRegion;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * {@link AbstractNioChannel} base class for {@link Channel}s that operate on bytes.
 */
public abstract class AbstractNioByteChannel extends AbstractNioChannel
{
    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
    private static final String EXPECTED_TYPES =
            " (expected: " + StringUtil.simpleClassName(ByteBuf.class) + ", " +
                    StringUtil.simpleClassName(FileRegion.class) + ')';

    private Runnable flushTask;

    /**
     * Create a new instance
     *
     * @param parent the parent {@link Channel} by which this instance was created. May be {@code null}
     * @param ch     the underlying {@link SelectableChannel} on which it operates
     *               <p>
     *               客户端感兴趣的事件是从服务端读取数据
     */
    protected AbstractNioByteChannel(Channel parent, SelectableChannel ch)
    {
        super(parent, ch, SelectionKey.OP_READ);
    }

    /**
     * Shutdown the input side of the channel.
     */
    protected abstract ChannelFuture shutdownInput();

    protected boolean isInputShutdown0()
    {
        return false;
    }

    // 客户端channel的抽象
    @Override
    protected AbstractNioUnsafe newUnsafe()
    {
        return new NioByteUnsafe();
    }

    @Override
    public ChannelMetadata metadata()
    {
        return METADATA;
    }

    protected class NioByteUnsafe extends AbstractNioUnsafe
    {

        private void closeOnRead(ChannelPipeline pipeline)
        {
            if (!isInputShutdown0())
            {
                if (Boolean.TRUE.equals(config().getOption(ChannelOption.ALLOW_HALF_CLOSURE)))
                {
                    shutdownInput();
                    pipeline.fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
                } else
                {
                    close(voidPromise());
                }
            } else
            {
                pipeline.fireUserEventTriggered(ChannelInputShutdownReadComplete.INSTANCE);
            }
        }

        private void handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close,
                                         RecvByteBufAllocator.Handle allocHandle)
        {
            if (byteBuf != null)
            {
                if (byteBuf.isReadable())
                {
                    readPending = false;
                    pipeline.fireChannelRead(byteBuf);
                } else
                {
                    byteBuf.release();
                }
            }
            allocHandle.readComplete();
            pipeline.fireChannelReadComplete();
            pipeline.fireExceptionCaught(cause);
            if (close || cause instanceof IOException)
            {
                closeOnRead(pipeline);
            }
        }

        @Override
        public final void read()
        {
            final ChannelConfig config = config();
            final ChannelPipeline pipeline = pipeline();
            final ByteBufAllocator allocator = config.getAllocator();
            /**
             * 该对象用来计算字节数，根据预测和计算最佳的缓冲区大小,避免频繁扩容或者浪费内存
             * 一般是 AdaptiveRecvByteBufAllocator 类型，自适应计算缓冲分配
             */
            final RecvByteBufAllocator.Handle allocHandle = recvBufAllocHandle();
            allocHandle.reset(config);

            ByteBuf byteBuf = null;
            boolean close = false;
            try
            {
                do
                {
                    //获取缓冲对象，根据allocHandle计算出要缓冲区的大小
                    byteBuf = allocHandle.allocate(allocator);
                    //将Channel的内容读取到byteBuf中，并记录读取到的字节数
                    allocHandle.lastBytesRead(doReadBytes(byteBuf));

                    // 如果读到的字节数小于等于0，清理引用和跳出循环
                    if (allocHandle.lastBytesRead() <= 0)
                    {
                        byteBuf.release();
                        byteBuf = null;
                        // 如果远程已经关闭连接
                        close = allocHandle.lastBytesRead() < 0;
                        break;
                    }
                    allocHandle.incMessagesRead(1);
                    readPending = false;
                    pipeline.fireChannelRead(byteBuf);
                    byteBuf = null;

                    /**
                     * 如果满足了跳出条件，也要结束循环，不能无限循环，默认16 次。
                     * 为什么呢？因为由于 TCP 传输如果包过大的话，丢失的风险会更大，导致重传，所以，大的数据流会分成多次传输
                     */
                } while (allocHandle.continueReading());

                //跳出循环后，调用 allocHandle 的 readComplete 方法，表示读取已完成,并记录读取记录，用于下次分配合理内存
                allocHandle.readComplete();
                pipeline.fireChannelReadComplete();

                if (close)
                {
                    closeOnRead(pipeline);
                }
            } catch (Throwable t)
            {
                handleReadException(pipeline, byteBuf, t, close, allocHandle);
            } finally
            {
                // Check if there is a readPending which was not processed yet.
                // This could be for two reasons:
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelRead(...) method
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelReadComplete(...) method
                //
                // See https://github.com/netty/netty/issues/2254
                if (!readPending && !config.isAutoRead())
                {
                    removeReadOp();
                }
            }
        }
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception
    {
        int writeSpinCount = -1;

        boolean setOpWrite = false;
        // 整体是无限循环, 过滤ByteBuf
        for (; ; )
        {
            Object msg = in.current();
            if (msg == null)
            {
                // 如果写缓存区中没有可写的数据，取消注册写事件
                clearOpWrite();
                // Directly return here so incompleteWrite(...) is not called.
                return;
            }

            if (msg instanceof ByteBuf)
            {
                // 第三部分,jdk底层, 进行自旋的写
                ByteBuf buf = (ByteBuf) msg;
                int readableBytes = buf.readableBytes();
                if (readableBytes == 0)
                {
                    // 当前的 ByteBuf 中,没有可写的, 直接remove掉
                    in.remove();
                    continue;
                }

                boolean done = false;
                long flushedAmount = 0;
                if (writeSpinCount == -1)
                {
                    // 获取自旋锁, netty使用它进行
                    writeSpinCount = config().getWriteSpinCount();
                }
                // 这个for循环是在自旋尝试往 jdk底层的 ByteBuf写入数据
                for (int i = writeSpinCount - 1; i >= 0; i--)
                {

                    int localFlushedAmount = doWriteBytes(buf);

                    if (localFlushedAmount == 0)
                    {
                        setOpWrite = true;
                        break;
                    }
                    flushedAmount += localFlushedAmount;
                    if (!buf.isReadable())
                    {
                        done = true;
                        break;
                    }
                }

                in.progress(flushedAmount);

                if (done)
                {
                    in.remove();
                } else
                {
                    // Break the loop and so incompleteWrite(...) is called.
                    break;
                }
            } else if (msg instanceof FileRegion)
            {
                FileRegion region = (FileRegion) msg;
                boolean done = region.transferred() >= region.count();

                if (!done)
                {
                    long flushedAmount = 0;
                    if (writeSpinCount == -1)
                    {
                        writeSpinCount = config().getWriteSpinCount();
                    }

                    for (int i = writeSpinCount - 1; i >= 0; i--)
                    {
                        long localFlushedAmount = doWriteFileRegion(region);
                        if (localFlushedAmount == 0)
                        {
                            setOpWrite = true;
                            break;
                        }

                        flushedAmount += localFlushedAmount;
                        if (region.transferred() >= region.count())
                        {
                            done = true;
                            break;
                        }
                    }

                    in.progress(flushedAmount);
                }

                if (done)
                {
                    in.remove();
                } else
                {
                    // Break the loop and so incompleteWrite(...) is called.
                    break;
                }
            } else
            {
                // Should not reach here.
                throw new Error();
            }
        }
        incompleteWrite(setOpWrite);
    }

    @Override
    protected final Object filterOutboundMessage(Object msg)
    {
        if (msg instanceof ByteBuf)
        {
            ByteBuf buf = (ByteBuf) msg;

            //如果是直接缓冲区就返回
            if (buf.isDirect())
            {
                return msg;
            }

            //否则封装成直接缓冲区就可以零拷贝
            return newDirectBuffer(buf);
        }

        //文件缓冲区也可以零拷贝
        if (msg instanceof FileRegion)
        {
            return msg;
        }

        //剩下的就不支持了
        throw new UnsupportedOperationException(
                "unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
    }

    protected final void incompleteWrite(boolean setOpWrite)
    {
        // Did not write completely.
        if (setOpWrite)
        {
            setOpWrite();
        } else
        {
            // Schedule flush again later so other tasks can be picked up in the meantime
            Runnable flushTask = this.flushTask;
            if (flushTask == null)
            {
                flushTask = this.flushTask = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        flush();
                    }
                };
            }
            eventLoop().execute(flushTask);
        }
    }

    /**
     * Write a {@link FileRegion}
     *
     * @param region the {@link FileRegion} from which the bytes should be written
     * @return amount       the amount of written bytes
     */
    protected abstract long doWriteFileRegion(FileRegion region) throws Exception;

    /**
     * Read bytes into the given {@link ByteBuf} and return the amount.
     */
    protected abstract int doReadBytes(ByteBuf buf) throws Exception;

    /**
     * Write bytes form the given {@link ByteBuf} to the underlying {@link java.nio.channels.Channel}.
     *
     * @param buf the {@link ByteBuf} from which the bytes should be written
     * @return amount       the amount of written bytes
     */
    protected abstract int doWriteBytes(ByteBuf buf) throws Exception;

    protected final void setOpWrite()
    {
        final SelectionKey key = selectionKey();
        // Check first if the key is still valid as it may be canceled as part of the deregistration
        // from the EventLoop
        // See https://github.com/netty/netty/issues/2104
        if (!key.isValid())
        {
            return;
        }
        final int interestOps = key.interestOps();
        if ((interestOps & SelectionKey.OP_WRITE) == 0)
        {
            key.interestOps(interestOps | SelectionKey.OP_WRITE);
        }
    }

    protected final void clearOpWrite()
    {
        final SelectionKey key = selectionKey();
        // Check first if the key is still valid as it may be canceled as part of the deregistration
        // from the EventLoop
        // See https://github.com/netty/netty/issues/2104
        if (!key.isValid())
        {
            return;
        }
        final int interestOps = key.interestOps();
        if ((interestOps & SelectionKey.OP_WRITE) != 0)
        {
            key.interestOps(interestOps & ~SelectionKey.OP_WRITE);
        }
    }
}
