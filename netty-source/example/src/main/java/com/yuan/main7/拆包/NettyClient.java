package com.epichust.main7.拆包;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

public class NettyClient
{
    public static void main(String[] args)
    {
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try
        {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception
                        {
                            ch.pipeline().addLast(new MessageEncoder());
                            ch.pipeline().addLast(new MyClientHandler());
                        }
                    });
            bootstrap.connect("127.0.0.1", 3333).sync().channel().closeFuture().sync();
        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            workGroup.shutdownGracefully();
        }
    }
}
