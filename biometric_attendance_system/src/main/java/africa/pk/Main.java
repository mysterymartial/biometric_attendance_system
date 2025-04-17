package africa.pk;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        System.setProperty("jasypt.encryptor.password", "secret key");
        SpringApplication.run(Main.class, args);
    }


}