package com.ynan.main;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

/**
 * @Author yuannan
 * @Date 2021/12/15 16:11
 */
public class ProducerMain {

    public static void main(String[] args) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {

        DefaultMQProducer producer = new DefaultMQProducer("groupName");
        producer.setNamespace("namespaceName");
        producer.setNamesrvAddr("47.100.24.106:9876");

        producer.start();

        Message message = new Message("TopicAyyy", "TAG", "hello world".getBytes());
        message.setKeys("xxx-key");

        SendResult sendResult = producer.send(message);
        System.out.println(sendResult);

        producer.shutdown();
    }
}
