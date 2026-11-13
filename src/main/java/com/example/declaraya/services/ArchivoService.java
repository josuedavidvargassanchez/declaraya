package com.example.declaraya.services;

import com.example.declaraya.exceptions.RecursoNoEncontradoException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.declaraya.models.Archivo;
import com.example.declaraya.models.Mensaje;
import com.example.declaraya.repositories.ArchivoRepository;

@Service 
public class ArchivoService {
    public static class ArchivoNoPermitidoException extends RuntimeException{
        public ArchivoNoPermitidoException(String mensaje){
            super(mensaje);
        }
    }

    private static final Set<String> EXTENSIONES = Set.of("pdf", "jpg", "jpeg", "png");

    private final ArchivoRepository archivoRepository;
    private final Path carpeta;

    public ArchivoService(ArchivoRepository archivoRepository, @Value("${declaraya.uploads.dir:uploads}") String directorio) throws IOException{
        this.archivoRepository = archivoRepository;
        this.carpeta = Paths.get(directorio).toAbsolutePath().normalize();
        Files.createDirectories(this.carpeta);
    }

    public void validar(MultipartFile archivo){
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !EXTENSIONES.contains(extension(nombre))){
            throw new ArchivoNoPermitidoException("Solo se permiten archivos PDF, JPG, o PNG");
        }
    }

    // el campo "url" de Archivo guarda el nombre con el que se almaceno en disco
    public Archivo guardar(Mensaje mensaje, MultipartFile archivo) throws IOException{
        String original = StringUtils.getFilename(StringUtils.cleanPath(archivo.getOriginalFilename()));
        String guardado = UUID.randomUUID() + "." + extension(original);
        try (InputStream in = archivo.getInputStream()){
            Files.copy(in, carpeta.resolve(guardado), StandardCopyOption.REPLACE_EXISTING);
        }
        return archivoRepository.save(new Archivo(mensaje, original, guardado));
    }

    public List<Archivo> listarPorMensaje(Mensaje mensaje){
        return archivoRepository.findByMensaje(mensaje);
    }
    
    public Archivo buscarPorId(Long id){
        return archivoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Archivo no encontrado"));
    }

    public Resource cargar(Archivo archivo) throws IOException{
        Path ruta = carpeta.resolve(archivo.getUrl()).normalize();
        if (!ruta.startsWith(carpeta) || !Files.exists(ruta)) {
            throw new RecursoNoEncontradoException("Archivo no encontrado");
        }
        return new UrlResource(ruta.toUri());
    }

    private static String extension (String nombre){
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase();
    }

}
