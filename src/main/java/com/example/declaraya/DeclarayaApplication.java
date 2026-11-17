package com.example.declaraya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // recordatorio diario de fechas de entrega (RecordatorioService)
public class DeclarayaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeclarayaApplication.class, args);
	}

}
