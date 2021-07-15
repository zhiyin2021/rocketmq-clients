package org.apache.rocketmq.client.impl.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.rocketmq.client.message.MessageQueue;
import org.apache.rocketmq.client.route.Partition;

@Slf4j
@ToString
@EqualsAndHashCode
public class TopicAssignment {
    private static final ThreadLocal<AtomicInteger> partitionIndex = new ThreadLocal<AtomicInteger>();

    @Getter
    private final List<Assignment> assignmentList;

    public TopicAssignment(List<apache.rocketmq.v1.Assignment> assignmentList) {
        this.assignmentList = new ArrayList<Assignment>();

        for (apache.rocketmq.v1.Assignment item : assignmentList) {
            MessageQueue messageQueue =
                    new MessageQueue(new Partition(item.getPartition()));

            MessageRequestMode mode = MessageRequestMode.POP;
            switch (item.getMode()) {
                case PULL:
                    mode = MessageRequestMode.PULL;
                    break;
                case POP:
                    mode = MessageRequestMode.POP;
                    break;
                default:
                    log.warn("Unknown message request mode={}, default to pop.", item.getMode());
            }
            this.assignmentList.add(new Assignment(messageQueue, mode));
        }
    }

    public static int getNextPartitionIndex() {
        if (null == partitionIndex.get()) {
            partitionIndex.set(new AtomicInteger(RandomUtils.nextInt()));
        }
        return partitionIndex.get().getAndIncrement();
    }
}
