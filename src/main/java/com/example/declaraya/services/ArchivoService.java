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
import org.springframework.http.MediaType;
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

    // ------------------------------------------- archivos fuera del chat
    // soportes de verificacion (uploads/verificaciones) y documentos del cliente (uploads/documentos).
    // Se guardan con nombre UUID y nunca se acepta una ruta que salga de la subcarpeta.

    public static final long MAX_BYTES = 10L * 1024 * 1024;
    public static final String CARPETA_VERIFICACIONES = "verificaciones";
    public static final String CARPETA_DOCUMENTOS = "documentos";
    public static final String CARPETA_ENTREGAS = "entregas";

    private Path subcarpeta(String nombre) throws IOException {
        Path destino = carpeta.resolve(nombre).normalize();
        if (!destino.startsWith(carpeta)) {
            throw new IOException("Carpeta no permitida");
        }
        Files.createDirectories(destino);
        return destino;
    }

    // mismas reglas que los archivos del chat: PDF, JPG, JPEG o PNG de maximo 10 MB
    public void validarSoporte(MultipartFile archivo) {
        validar(archivo);
        if (archivo.getSize() > MAX_BYTES) {
            throw new ArchivoNoPermitidoException("El archivo no puede superar 10 MB");
        }
    }

    /** Valida y guarda el archivo en la subcarpeta; devuelve el nombre UUID con el que quedo en disco. */
    public String guardarEn(String sub, MultipartFile archivo) throws IOException {
        validarSoporte(archivo);
        String original = StringUtils.getFilename(StringUtils.cleanPath(archivo.getOriginalFilename()));
        String guardado = UUID.randomUUID() + "." + extension(original);
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, subcarpeta(sub).resolve(guardado), StandardCopyOption.REPLACE_EXISTING);
        }
        return guardado;
    }

    public Resource cargarDe(String sub, String nombre) throws IOException {
        if (nombre == null || nombre.isBlank()) {
            throw new RecursoNoEncontradoException("Documento no adjuntado");
        }
        Path base = subcarpeta(sub);
        Path ruta = base.resolve(nombre).normalize();
        if (!ruta.startsWith(base) || !Files.exists(ruta)) {
            throw new RecursoNoEncontradoException("Documento no encontrado");
        }
        return new UrlResource(ruta.toUri());
    }

    public void eliminarDe(String sub, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        try {
            Path base = subcarpeta(sub);
            Path ruta = base.resolve(nombre).normalize();
            if (ruta.startsWith(base)) {
                Files.deleteIfExists(ruta);
            }
        } catch (IOException e) {
            // si no se borra el archivo anterior no pasa nada
        }
    }

    public String guardarSoporteVerificacion(MultipartFile archivo) throws IOException {
        return guardarEn(CARPETA_VERIFICACIONES, archivo);
    }

    public Resource cargarSoporteVerificacion(String nombre) throws IOException {
        return cargarDe(CARPETA_VERIFICACIONES, nombre);
    }

    public void eliminarSoporteVerificacion(String nombre) {
        eliminarDe(CARPETA_VERIFICACIONES, nombre);
    }

    public static MediaType tipoDeContenido(String nombre) {
        String ext = nombre == null ? "" : extension(nombre);
        return switch (ext) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private static String extension (String nombre){
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase();
    }

}
