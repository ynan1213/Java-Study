package io.netty.example.text.zdyxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

/**
 * @Author: Changwu
 * @Date: 2019/7/21 21:31
 */
public class MyClientInitializer extends ChannelInitializer{
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new MyPersonEncoder());
        pipeline.addLast(new MyPersonDecoder());
        pipeline.addLast(new MyClientHandler());
    }
}
