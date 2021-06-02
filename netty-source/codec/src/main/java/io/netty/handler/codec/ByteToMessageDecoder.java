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
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.util.internal.StringUtil;

import java.util.List;

/**
 * {@link ChannelInboundHandlerAdapter} which decodes bytes in a stream-like fashion from one {@link ByteBuf} to an
 * other Message type.
 * 数据在网络中传递, 一旦进入netty就会被转换成 ByteBuf 类型, 这个解码器的基类 作用就是 将 ByteBuf 类型的数据,转换成 另一种消息的类型
 * For example here is an implementation which reads all readable bytes from
 * the input {@link ByteBuf} and create a new {@link ByteBuf}.
 * 比如说, 下面的示例就是从ByteBuf中读取所有可读的数据,创建一个新的 ByteBuf
 * <pre>
 *     public class SquareDecoder extends {@link ByteToMessageDecoder} {
 *         {@code @Override}
 *         public void decode({@link ChannelHandlerContext} ctx, {@link ByteBuf} in, List&lt;Object&gt; out)
 *                 throws {@link Exception} {
 *             out.add(in.readBytes(in.readableBytes()));
 *         }
 *     }
 *
 *      ctx:   每一个编解码器都是一个 handler
 *      in :   从in中读取数据
 *      List<Onject> out :  把in中的添加到out中
 * </pre>
 *
 * <h3>Frame detection</h3> // 帧的检测,tcp的粘包和拆包
 * <p>
 * Generally frame detection should be handled earlier in the pipeline by adding a
 * {@link DelimiterBasedFrameDecoder}, {@link FixedLengthFrameDecoder}, {@link LengthFieldBasedFrameDecoder},
 * or {@link LineBasedFrameDecoder}.
 * <p>
 * 上面的几个 decoder 比如第一个, 根据 Delimiter Based FrameDecoder 根据分隔符 进行帧的检测, 解决拆包和粘包问题
 * <p>
 * If a custom frame decoder is required, then one needs to be careful when implementing
 * one with {@link ByteToMessageDecoder}. Ensure there are enough bytes in the buffer for a
 * complete frame by checking {@link ByteBuf#readableBytes()}. If there are not enough bytes
 * for a complete frame, return without modifying the reader index to allow more bytes to arrive.
 * <p>
 * 在实现 ByteToMessageDecoder 是要非常的小心, 要使用 readableBytes() 确保在 ByteBuf中存在 足够的字节构成一个完成的帧
 * 比如说,我们的readInt操作,如果说ByteBuf总还有三个字节, 而readInt需要的是四个, 这就会发生错误 , 所以就得等待下一次数据的到来
 * <p>
 * To check for complete frames without modifying the reader index, use methods like {@link ByteBuf#getInt(int)}.
 * One <strong>MUST</strong> use the reader index when using methods like {@link ByteBuf#getInt(int)}.
 * For example calling <tt>in.getInt(0)</tt> is assuming the frame starts at the beginning of the buffer, which
 * is not always the case. Use <tt>in.getInt(in.readerIndex())</tt> instead.
 * 想要检查一个完整的 帧,而不会改变读索引, 就用 getInt(int)  我们知道这是个根据 绝对路径 获取指定位置上的字节数据 而不会改变readIndex值
 * 举个例子, 你调用了getInt(0) 说明你认为可读的 帧的开始位置是0 , 但是实际情况往往不是这样的 , 所以建议我们从in.readerIndex() 安全的读取
 * <h3>Pitfalls</h3>
 * <p>
 * Be aware that sub-classes of {@link ByteToMessageDecoder} <strong>MUST NOT</strong>
 * annotated with {@link @Sharable}.
 * <p>
 * Some methods such as {@link ByteBuf#readBytes(int)} will cause a memory leak if the returned buffer
 * is not released or added to the <tt>out</tt> {@link List}. Use derived buffers like {@link ByteBuf#readSlice(int)}
 * to avoid leaking memory.
 * 一些方法, 比如说  readBytes(int) , 它可能会导致内存的泄露, 如果我们没释放buffer 或者把它添加到out中, 所以请使用 衍生的buffer #readSlice(int)
 */
public abstract class ByteToMessageDecoder extends ChannelInboundHandlerAdapter
{
    /**
     * Cumulate {@link ByteBuf}s by merge them into one {@link ByteBuf}'s, using memory copies.
     * <p>
     * 合并累加器
     */
    public static final Cumulator MERGE_CUMULATOR = new Cumulator()
    {
        @Override
        public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in)
        {
            final ByteBuf buffer;
            //cumulation的可写下标+当前要读取的in的可读容量之和是不是大于cumulation的最大容量
            // 这样做的就是防止cumulation的ByteBuf容量溢出，如果大于了，则说明cumulation需要扩容
            if (cumulation.writerIndex() > cumulation.maxCapacity() - in.readableBytes() || cumulation.refCnt() > 1 || cumulation.isReadOnly())
            {
                // Expand cumulation (by replace it) when either there is not more room in the buffer
                // or if the refCnt is greater then 1 which may happen when the user use slice().retain() or
                // duplicate().retain() or if its read-only.
                //
                // See:
                // - https://github.com/netty/netty/issues/2327
                // - https://github.com/netty/netty/issues/1764
                // 扩容
                buffer = expandCumulation(alloc, cumulation, in.readableBytes());
            } else
            {
                buffer = cumulation;
            }
            // 往 ByteBuf中写入数据 完成累加
            buffer.writeBytes(in);
            // 累加完成之后，原数据 释放掉
            in.release();
            return buffer;
        }
    };

    /**
     * Cumulate {@link ByteBuf}s by add them to a {@link CompositeByteBuf} and so do no memory copy whenever possible.
     * Be aware that {@link CompositeByteBuf} use a more complex indexing implementation so depending on your use-case
     * and the decoder implementation this may be slower then just use the {@link #MERGE_CUMULATOR}.
     * <p>
     * 复合累加器
     */
    public static final Cumulator COMPOSITE_CUMULATOR = new Cumulator()
    {
        @Override
        public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in)
        {
            ByteBuf buffer;
            if (cumulation.refCnt() > 1)
            {
                // Expand cumulation (by replace it) when the refCnt is greater then 1 which may happen when the user
                // use slice().retain() or duplicate().retain().
                //
                // See:
                // - https://github.com/netty/netty/issues/2327
                // - https://github.com/netty/netty/issues/1764
                buffer = expandCumulation(alloc, cumulation, in.readableBytes());
                buffer.writeBytes(in);
                in.release();
            } else
            {
                CompositeByteBuf composite;
                if (cumulation instanceof CompositeByteBuf)
                {
                    composite = (CompositeByteBuf) cumulation;
                } else
                {
                    composite = alloc.compositeBuffer(Integer.MAX_VALUE);
                    composite.addComponent(true, cumulation);
                }
                composite.addComponent(true, in);
                buffer = composite;
            }
            return buffer;
        }
    };

    private static final byte STATE_INIT = 0;//初始状态
    private static final byte STATE_CALLING_CHILD_DECODE = 1;//正在调用子类解码
    private static final byte STATE_HANDLER_REMOVED_PENDING = 2;//处理器待删除

    ByteBuf cumulation;//累加缓冲区,如果有不能拼成一个消息的数据会放入这个缓冲区里，等待下一次继续拼
    private Cumulator cumulator = MERGE_CUMULATOR;//累加缓冲区怎么累加，就需要有累加器，默认是合并累加器
    private boolean singleDecode;//是否只解码一次
    private boolean decodeWasNull;
    private boolean first;//是否第一次解码
    /**
     * A bitmask where the bits are defined as
     * <ul>
     *     <li>{@link #STATE_INIT}</li>
     *     <li>{@link #STATE_CALLING_CHILD_DECODE}</li>
     *     <li>{@link #STATE_HANDLER_REMOVED_PENDING}</li>
     * </ul>
     */
    private byte decodeState = STATE_INIT;
    private int discardAfterReads = 16;//读取16个字节后丢弃已读的
    private int numReads;//cumulation读取数据的次数

    protected ByteToMessageDecoder()
    {
        ensureNotSharable();
    }

    /**
     * If set then only one message is decoded on each {@link #channelRead(ChannelHandlerContext, Object)}
     * call. This may be useful if you need to do some protocol upgrade and want to make sure nothing is mixed up.
     * <p>
     * Default is {@code false} as this has performance impacts.
     */
    public void setSingleDecode(boolean singleDecode)
    {
        this.singleDecode = singleDecode;
    }

    /**
     * If {@code true} then only one message is decoded on each
     * {@link #channelRead(ChannelHandlerContext, Object)} call.
     * <p>
     * Default is {@code false} as this has performance impacts.
     */
    public boolean isSingleDecode()
    {
        return singleDecode;
    }

    /**
     * Set the {@link Cumulator} to use for cumulate the received {@link ByteBuf}s.
     */
    public void setCumulator(Cumulator cumulator)
    {
        if (cumulator == null)
        {
            throw new NullPointerException("cumulator");
        }
        this.cumulator = cumulator;
    }

    /**
     * Set the number of reads after which {@link ByteBuf#discardSomeReadBytes()} are called and so free up memory.
     * The default is {@code 16}.
     */
    public void setDiscardAfterReads(int discardAfterReads)
    {
        if (discardAfterReads <= 0)
        {
            throw new IllegalArgumentException("discardAfterReads must be > 0");
        }
        this.discardAfterReads = discardAfterReads;
    }

    /**
     * Returns the actual number of readable bytes in the internal cumulative
     * buffer of this decoder. You usually do not need to rely on this value
     * to write a decoder. Use it only when you must use it at your own risk.
     * This method is a shortcut to {@link #internalBuffer() internalBuffer().readableBytes()}.
     */
    protected int actualReadableBytes()
    {
        return internalBuffer().readableBytes();
    }

    /**
     * Returns the internal cumulative buffer of this decoder. You usually
     * do not need to access the internal buffer directly to write a decoder.
     * Use it only when you must use it at your own risk.
     */
    protected ByteBuf internalBuffer()
    {
        if (cumulation != null)
        {
            return cumulation;
        } else
        {
            return Unpooled.EMPTY_BUFFER;
        }
    }

    @Override
    public final void handlerRemoved(ChannelHandlerContext ctx) throws Exception
    {
        if (decodeState == STATE_CALLING_CHILD_DECODE)
        {
            decodeState = STATE_HANDLER_REMOVED_PENDING;
            return;
        }
        ByteBuf buf = cumulation;
        if (buf != null)
        {
            // Directly set this to null so we are sure we not access it in any other method here anymore.
            cumulation = null;

            int readable = buf.readableBytes();
            if (readable > 0)
            {
                ByteBuf bytes = buf.readBytes(readable);
                buf.release();
                ctx.fireChannelRead(bytes);
            } else
            {
                buf.release();
            }

            numReads = 0;
            ctx.fireChannelReadComplete();
        }
        handlerRemoved0(ctx);
    }

    /**
     * Gets called after the {@link ByteToMessageDecoder} was removed from the actual context and it doesn't handle
     * events anymore.
     */
    protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception
    {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        //只处理字节缓冲区类型的
        if (msg instanceof ByteBuf)
        {
            // 从对象池中取出一个List
            CodecOutputList out = CodecOutputList.newInstance();
            try
            {
                ByteBuf data = (ByteBuf) msg;
                //如果为空，则说明这是第一次进来的数据， 从没累加过
                first = cumulation == null;
                if (first)
                {
                    cumulation = data;
                } else
                {
                    // cumulator 是类型累加器，其实就是往 ByteBuf中 write数据，并且，当ByteBuf 内存不够时进行扩容
                    cumulation = cumulator.cumulate(ctx.alloc(), cumulation, data);
                }
                callDecode(ctx, cumulation, out);
            } catch (DecoderException e)
            {
                throw e;
            } catch (Throwable t)
            {
                throw new DecoderException(t);
            } finally
            {
                if (cumulation != null && !cumulation.isReadable())
                {// 如果累计区没有可读字节了
                    numReads = 0;// 将次数归零
                    cumulation.release();// 释放累计区
                    cumulation = null;// 等待 gc
                } else if (++numReads >= discardAfterReads)
                {//读取数据的次数大于阈值，则尝试丢弃已读的，避免占着内存
                    // We did enough reads already try to discard some bytes so we not risk to see a OOME.
                    // See https://github.com/netty/netty/issues/4275
                    numReads = 0;
                    discardSomeReadBytes();
                }

                int size = out.size();
                decodeWasNull = !out.insertSinceRecycled();
                fireChannelRead(ctx, out, size);
                out.recycle();
            }
        } else
        {
            //不是byteBuf类型则向下传播
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * Get {@code numElements} out of the {@link List} and forward these through the pipeline.
     */
    static void fireChannelRead(ChannelHandlerContext ctx, List<Object> msgs, int numElements)
    {
        if (msgs instanceof CodecOutputList)
        {
            fireChannelRead(ctx, (CodecOutputList) msgs, numElements);
        } else
        {
            for (int i = 0; i < numElements; i++)
            {
                ctx.fireChannelRead(msgs.get(i));
            }
        }
    }

    /**
     * Get {@code numElements} out of the {@link CodecOutputList} and forward these through the pipeline.
     */
    static void fireChannelRead(ChannelHandlerContext ctx, CodecOutputList msgs, int numElements)
    {
        for (int i = 0; i < numElements; i++)
        {
            ctx.fireChannelRead(msgs.getUnsafe(i));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
    {
        numReads = 0;
        discardSomeReadBytes();
        if (decodeWasNull)
        {
            decodeWasNull = false;
            if (!ctx.channel().config().isAutoRead())
            {
                ctx.read();
            }
        }
        ctx.fireChannelReadComplete();
    }

    protected final void discardSomeReadBytes()
    {
        if (cumulation != null && !first && cumulation.refCnt() == 1)
        {
            // discard some bytes if possible to make more room in the
            // buffer but only if the refCnt == 1  as otherwise the user may have
            // used slice().retain() or duplicate().retain().
            //
            // See:
            // - https://github.com/netty/netty/issues/2327
            // - https://github.com/netty/netty/issues/1764
            cumulation.discardSomeReadBytes();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        channelInputClosed(ctx, true);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        if (evt instanceof ChannelInputShutdownEvent)
        {
            // The decodeLast method is invoked when a channelInactive event is encountered.
            // This method is responsible for ending requests in some situations and must be called
            // when the input has been shutdown.
            channelInputClosed(ctx, false);
        }
        super.userEventTriggered(ctx, evt);
    }

    private void channelInputClosed(ChannelHandlerContext ctx, boolean callChannelInactive) throws Exception
    {
        CodecOutputList out = CodecOutputList.newInstance();
        try
        {
            channelInputClosed(ctx, out);
        } catch (DecoderException e)
        {
            throw e;
        } catch (Exception e)
        {
            throw new DecoderException(e);
        } finally
        {
            try
            {
                if (cumulation != null)
                {
                    cumulation.release();
                    cumulation = null;
                }
                int size = out.size();
                fireChannelRead(ctx, out, size);
                if (size > 0)
                {
                    // Something was read, call fireChannelReadComplete()
                    ctx.fireChannelReadComplete();
                }
                if (callChannelInactive)
                {
                    ctx.fireChannelInactive();
                }
            } finally
            {
                // Recycle in all cases
                out.recycle();
            }
        }
    }

    /**
     * Called when the input of the channel was closed which may be because it changed to inactive or because of
     * {@link ChannelInputShutdownEvent}.
     */
    void channelInputClosed(ChannelHandlerContext ctx, List<Object> out) throws Exception
    {
        if (cumulation != null)
        {
            callDecode(ctx, cumulation, out);
            decodeLast(ctx, cumulation, out);
        } else
        {
            decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
        }
    }

    /**
     * Called once data should be decoded from the given {@link ByteBuf}. This method will call
     * {@link #decode(ChannelHandlerContext, ByteBuf, List)} as long as decoding should take place.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param in  the {@link ByteBuf} from which to read data
     * @param out the {@link List} to which decoded messages should be added
     */
    protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    {
        try
        {
            while (in.isReadable())
            {// 如果累计区还有可读字节
                int outSize = out.size();
                if (outSize > 0)
                {//判断当前List是否有对象
                    //如果有对象, 则向下传播事件
                    fireChannelRead(ctx, out, outSize);
                    out.clear();

                    // Check if this handler was removed before continuing with decoding.
                    // If it was removed, it is not safe to continue to operate on the buffer.
                    //
                    // See:
                    // - https://github.com/netty/netty/issues/4635
                    //解码过程中如ctx被removed掉就break
                    if (ctx.isRemoved())
                    {
                        break;
                    }
                    outSize = 0;
                }

                int oldInputLength = in.readableBytes();

                // 调用 decode 方法，将成功解码后的数据放入道 out 数组中，可能会删除当前节点
                decodeRemovalReentryProtection(ctx, in, out);

                // Check if this handler was removed before continuing the loop.
                // If it was removed, it is not safe to continue to operate on the buffer.
                //
                // See https://github.com/netty/netty/issues/1664
                if (ctx.isRemoved())
                {
                    break;
                }

                if (outSize == out.size())
                {//List解析前大小 和解析后长度一样(什么没有解析出来)
                    if (oldInputLength == in.readableBytes())
                    {
                        //原来可读的长度==解析后可读长度
                        //说明没有读取数据(当前累加的数据并没有拼成一个完整的数据包)
                        break;
                    } else
                    {
                        //没有解析到数据, 但是进行读取了
                        continue;
                    }
                }

                // 来到这里就说明,已经解析出数据了 ,
                // 解析出数据了  就意味着in中的readIndex被子类改动了, 即 oldInputLength != in.readableBytes()
                // 如下现在还相等, 肯定是出问题了
                if (oldInputLength == in.readableBytes())
                {
                    throw new DecoderException(StringUtil.simpleClassName(getClass()) + ".decode() did not read anything but decoded a message.");
                }

                if (isSingleDecode())
                {
                    break;
                }
            }
        } catch (DecoderException e)
        {
            throw e;
        } catch (Throwable cause)
        {
            throw new DecoderException(cause);
        }
    }

    /**
     * Decode the from one {@link ByteBuf} to an other. This method will be called till either the input
     * {@link ByteBuf} has nothing to read when return from this method or till nothing was read from the input
     * {@link ByteBuf}.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param in  the {@link ByteBuf} from which to read data
     * @param out the {@link List} to which decoded messages should be added
     * @throws Exception is thrown if an error occurs
     */
    protected abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception;

    /**
     * Decode the from one {@link ByteBuf} to an other. This method will be called till either the input
     * {@link ByteBuf} has nothing to read when return from this method or till nothing was read from the input
     * {@link ByteBuf}.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param in  the {@link ByteBuf} from which to read data
     * @param out the {@link List} to which decoded messages should be added
     * @throws Exception is thrown if an error occurs
     */
    final void decodeRemovalReentryProtection(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        decodeState = STATE_CALLING_CHILD_DECODE;
        try
        {
            decode(ctx, in, out);
        } finally
        {
            boolean removePending = decodeState == STATE_HANDLER_REMOVED_PENDING;
            decodeState = STATE_INIT;
            if (removePending)
            {
                handlerRemoved(ctx);
            }
        }
    }

    /**
     * Is called one last time when the {@link ChannelHandlerContext} goes in-active. Which means the
     * {@link #channelInactive(ChannelHandlerContext)} was triggered.
     * <p>
     * By default this will just call {@link #decode(ChannelHandlerContext, ByteBuf, List)} but sub-classes may
     * override this for some special cleanup operation.
     */
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        if (in.isReadable())
        {
            // Only call decode() if there is something left in the buffer to decode.
            // See https://github.com/netty/netty/issues/4386
            decodeRemovalReentryProtection(ctx, in, out);
        }
    }

    static ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf cumulation, int readable)
    {
        ByteBuf oldCumulation = cumulation;
        cumulation = alloc.buffer(oldCumulation.readableBytes() + readable);
        cumulation.writeBytes(oldCumulation);
        oldCumulation.release();
        return cumulation;
    }

    /**
     * Cumulate {@link ByteBuf}s.
     */
    public interface Cumulator
    {
        /**
         * Cumulate the given {@link ByteBuf}s and return the {@link ByteBuf} that holds the cumulated bytes.
         * The implementation is responsible to correctly handle the life-cycle of the given {@link ByteBuf}s and so
         * call {@link ByteBuf#release()} if a {@link ByteBuf} is fully consumed.
         */
        ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in);
    }
}
