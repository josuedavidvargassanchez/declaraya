package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.declaraya.models.Usuario;
import com.example.declaraya.repositories.UsuarioRepository;

@Service 
public class FotoPerfilService {
    public static class FotoNoPermitidaException extends RuntimeException {
        public FotoNoPermitidaException (String mensaje){
            super(mensaje);
        }
        
    }

    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png");
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;
    private final Path carpeta;

    public FotoPerfilService(UsuarioRepository usuarioRepository, @Value("${declaraya.uploads.dir:uploads}") String directorio) throws IOException{
        this.usuarioRepository = usuarioRepository;
        this.carpeta = Paths.get(directorio).toAbsolutePath().normalize().resolve("perfiles");
        Files.createDirectories(this.carpeta);
    }

    public Usuario guardar(Usuario usuario, MultipartFile foto) throws IOException{
        String original = foto.getOriginalFilename();
        String ext = original == null ? "" : extension(original);
        if (!EXTENSIONES.contains(ext)) {
            throw new FotoNoPermitidaException("Solo se permiten imagenes JPG o PNG");
        }
        if (foto.getSize() > MAX_BYTES) {
            throw new FotoNoPermitidaException("La imagen no puede superar 2 MB");
        }
        String guardada = UUID.randomUUID() + "." + ext;
        try (InputStream in = foto.getInputStream()){
            Files.copy(in, carpeta.resolve(guardada), StandardCopyOption.REPLACE_EXISTING);
        }

        borrarArchivo(usuario.getFotoPerfil());
        usuario.setFotoPerfil(guardada);
        return usuarioRepository.save(usuario);
    }

    public Usuario eliminar(Usuario usuario){
        borrarArchivo(usuario.getFotoPerfil());
        usuario.setFotoPerfil(null);
        return usuarioRepository.save(usuario);
    }

    public Resource cargar(Usuario usuario) throws IOException{
        if (usuario.getFotoPerfil() == null) {
            throw new RecursoNoEncontradoException("El usuario no tiene foto");

        }
        Path ruta = carpeta.resolve(usuario.getFotoPerfil()).normalize();
        if (!ruta.startsWith(carpeta) || !Files.exists(ruta)) {
            throw new RecursoNoEncontradoException("Foto no encontrada");
        }
        return new UrlResource(ruta.toUri());
    }

    public MediaType tipoDe(Usuario usuario){
        String foto = usuario.getFotoPerfil();
        return foto != null && foto.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
    }

    private void borrarArchivo(String nombre){
        if (nombre == null) {
            return;
        }
        try {
            Path ruta = carpeta.resolve(nombre).normalize();
            if (ruta.startsWith(carpeta)) {
                Files.deleteIfExists(ruta);
            }
        } catch (IOException e) {
            // si no se borra la foto anterior, no pasa nada
        }



    }

    private static String extension(String nombre){
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase();
    }



}
