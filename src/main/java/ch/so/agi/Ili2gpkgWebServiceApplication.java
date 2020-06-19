package ch.so.agi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class Ili2gpkgWebServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(Ili2gpkgWebServiceApplication.class, args);
	}
}
