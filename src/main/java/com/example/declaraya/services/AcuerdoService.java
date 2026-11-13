package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import com.example.declaraya.enums.EstadoAcuerdo;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Acuerdo;
import com.example.declaraya.models.Chat;
import com.example.declaraya.models.Oferta;
import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.AcuerdoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class AcuerdoService {

    // comision de la plataforma: 10% del valor propuesto en la oferta aceptada
    private static final BigDecimal PORCENTAJE_COMISION = new BigDecimal("0.10");

    @Autowired
    private AcuerdoRepository acuerdoRepository;

    @Autowired
    private ChatService chatService;

    @Autowired 
    private DisputaService disputaService;

    @Autowired 
    private SupervisionService supervisionService;

    public Acuerdo crearDesdeOferta(Oferta oferta) {
        BigDecimal comision = oferta.getValorPropuesto() != null
                ? oferta.getValorPropuesto().multiply(PORCENTAJE_COMISION)
                : BigDecimal.ZERO;

        Acuerdo acuerdo = acuerdoRepository.save(new Acuerdo(oferta, comision));
        chatService.guardar(new Chat(acuerdo));
        return acuerdo;
    }

    public Acuerdo buscarPorId(Long id) {
        return acuerdoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Acuerdo no encontrado"));
    }

    public Acuerdo buscarPorOfertaId(Long ofertaId) {
        return acuerdoRepository.findByOfertaId(ofertaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Acuerdo no encontrado para esa oferta"));
    }

    public  Acuerdo finalizar(Acuerdo acuerdo){
        acuerdo.setEstado(EstadoAcuerdo.FINALIZADO);
        return acuerdoRepository.save(acuerdo);
    }

    public long contarActivosDe(Usuario contador){
        return acuerdoRepository.countByOfertaContadorAndEstado(contador, EstadoAcuerdo.ACTIVO);
    }

    public List<Acuerdo> listarDe(Usuario usuario) {
        if (usuario.getRol() == Rol.PERSONA) {
            return acuerdoRepository.findByOfertaSolicitudPersona(usuario);
        }
        return acuerdoRepository.findByOfertaContador(usuario);
    }

    public List<Acuerdo> listarTodos(){
        return acuerdoRepository.findAll();
    }

    public BigDecimal totalComisiones(){
        return listarTodos().stream().map(Acuerdo::getComisionPlataforma).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean esParticipante(Acuerdo acuerdo, Usuario usuario){
        Usuario persona = acuerdo.getOferta().getSolicitud().getPersona();
        Usuario contador = acuerdo.getOferta().getContador();
        return usuario.getId().equals(persona.getId()) || usuario.getId().equals(contador.getId());

    }

    // participantes y mentor siempre; el admin solo si hay una disputa abierta
    public boolean puedeVer(Acuerdo acuerdo, Usuario usuario) {
        if (esParticipante(acuerdo, usuario)) {
            return true;
        }
        if (usuario.getRol() == Rol.ADMIN) {
            return disputaService.hayDisputaAbierta(acuerdo);
        }
        if (usuario.getRol() == Rol.CONTADOR_PUBLICO) {
            return supervisionService.esMentorDe(usuario, acuerdo);
        }
        return false;
    }
}
