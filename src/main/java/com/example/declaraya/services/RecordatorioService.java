package com.example.declaraya.services;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.EtapaAcuerdo;
import com.example.declaraya.enums.TipoActividad;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.repositories.AcuerdoRepository;

/**
 * Cada dia a las 8:00 (hora de Bogota) avisa al cliente y al contador cuando faltan 2 dias o menos
 * para la fecha estimada de un acuerdo que aun no esta ENTREGADO ni FINALIZADO.
 * Acuerdo.recordatorioEnviado evita repetir el aviso.
 */
@Service
public class RecordatorioService {

    public static final int DIAS_AVISO = 2;
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final AcuerdoRepository acuerdoRepository;
    private final NotificacionService notificacionService;
    private final ActividadAcuerdoService actividadService;

    public RecordatorioService(AcuerdoRepository acuerdoRepository, NotificacionService notificacionService,
                               ActividadAcuerdoService actividadService) {
        this.acuerdoRepository = acuerdoRepository;
        this.notificacionService = notificacionService;
        this.actividadService = actividadService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Bogota")
    public void recordatorioDiario() {
        enviarRecordatorios(LocalDate.now(BOGOTA));
    }

    /** Devuelve cuantos recordatorios envio (separado del @Scheduled para poder probarlo). */
    public int enviarRecordatorios(LocalDate hoy) {
        int enviados = 0;
        for (Acuerdo acuerdo : acuerdoRepository.findByEstadoAndRecordatorioEnviadoFalse(EstadoAcuerdo.ACTIVO)) {
            LocalDate fecha = acuerdo.getFechaEntregaEstimada();
            EtapaAcuerdo etapa = acuerdo.getEtapa();
            if (fecha == null || etapa == EtapaAcuerdo.ENTREGADO || etapa == EtapaAcuerdo.FINALIZADO) {
                continue;
            }
            long dias = ChronoUnit.DAYS.between(hoy, fecha);
            if (dias > DIAS_AVISO) {
                continue;
            }
            String cuando = dias < 0 ? "ya pasó" : (dias == 0 ? "es hoy" : "es en " + dias + " día(s)");
            String titulo = acuerdo.getOferta().getSolicitud().getTitulo();
            String mensaje = "Recordatorio: la fecha estimada de entrega de \"" + titulo + "\" " + cuando;
            String enlace = "/acuerdos/" + acuerdo.getId();
            notificacionService.notificar(acuerdo.getOferta().getSolicitud().getPersona(), mensaje, enlace);
            notificacionService.notificar(acuerdo.getOferta().getContador(), mensaje, enlace);
            acuerdo.setRecordatorioEnviado(true);
            acuerdoRepository.save(acuerdo);
            actividadService.registrar(acuerdo, null, TipoActividad.RECORDATORIO,
                    "Recordatorio enviado a ambas partes: la fecha estimada (" + fecha + ") " + cuando);
            enviados++;
        }
        return enviados;
    }
}
