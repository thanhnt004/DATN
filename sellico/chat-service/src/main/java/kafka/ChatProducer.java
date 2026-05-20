package kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatProducer {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    public void sendMessage(String topic,String key, String message){
        kafkaTemplate.send(topic,key, message);
    }
}
