package ticketsystem;

import java.time.LocalDateTime;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketSystemApplication {
    public static void main(String[] args) {
        //for vm
        System.setProperty("user.timezone", "Asia/Jerusalem");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jerusalem"));
        //for check
        System.out.println("timezone = " + TimeZone.getDefault().getID());
        System.out.println("now = " + LocalDateTime.now());
        SpringApplication.run(TicketSystemApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
