package com.quinielas.del.canario.api.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Abstracción del backend físico de almacenamiento de archivos.
 *
 * Implementaciones disponibles (seleccionadas con {@code app.storage.provider}):
 *  - {@link LocalFileStorageService} ("local", por defecto) → disco del servidor.
 *  - {@link OciS3StorageService}     ("oci")                → Oracle Object Storage
 *    (API S3-compatible), recomendado para producción (archivos persistentes,
 *    compartidos entre instancias, sin depender del filesystem del contenedor).
 *
 * {@code FileStorageService} (capa de negocio: validaciones, nombres de archivo)
 * es el único consumidor de esta interfaz; el resto de la aplicación no conoce
 * el backend concreto.
 */
public interface StorageService {

    /**
     * Guarda un archivo bajo la "carpeta" lógica indicada (p.ej. "perfiles",
     * "comprobantes"). En almacenamiento local es un subdirectorio físico;
     * en OCI es un prefijo dentro del bucket (objeto "carpeta/nombreArchivo").
     */
    void guardar(String carpeta, String nombreArchivo, MultipartFile file) throws IOException;

    /** Carga el archivo como {@link Resource} para servirlo/descargarlo. */
    Resource cargar(String carpeta, String nombreArchivo) throws IOException;

    /** Elimina el archivo si existe; no lanza error si no existe. */
    void eliminar(String carpeta, String nombreArchivo);

    /**
     * Devuelve una URL pública y directa al archivo, o {@code null} si el
     * backend no soporta/expone URLs públicas para esa carpeta (por ejemplo,
     * en almacenamiento local donde el archivo se sirve como recurso estático
     * de Spring, o para archivos privados como los comprobantes).
     */
    String obtenerUrlPublica(String carpeta, String nombreArchivo);
}

