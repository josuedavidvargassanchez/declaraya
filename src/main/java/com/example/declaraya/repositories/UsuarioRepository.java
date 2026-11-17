package com.example.declaraya.repositories;

import com.example.declaraya.enums.EstadoVerificacion;
import com.example.declaraya.enums.Rol;
import com.example.declaraya.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByRol(Rol rol);

    List<Usuario> findByMentor(Usuario mentor);

    long countByRol(Rol rol);

    long countByRolAndVerificado(Rol rol, boolean verificado);

    List<Usuario> findByRolInAndEstadoVerificacionIn(Collection<Rol> roles, Collection<EstadoVerificacion> estados);

    long countByRolInAndEstadoVerificacion(Collection<Rol> roles, EstadoVerificacion estado);

}
