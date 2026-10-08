package com.digital.reception.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dte_recepcion")
public class DteEntity {
    @Id
    @Column(name = "codigo_generacion", nullable = false, length = 36)
    private String codigoGeneracion;

    @Column(name = "nit_emisor", length = 14)
    private String nitEmisor;

    @Column(name = "tdte", length = 2)
    private String tdte;

    @Column(name = "ambiente", nullable = false, length = 2)
    private String ambiente;

    @Column(name = "numero_control", length = 31)
    private String numeroControl;

    @Column(name = "sello_recibido", length = 40)
    private String selloRecibido;

    @Column(name = "estado", nullable = false, length = 16)
    private String estado;

    @Column(name = "clasifica_msg", length = 2)
    private String clasificaMsg;

    @Column(name = "codigo_msg", length = 32)
    private String codigoMsg;

    @Column(name = "descripcion_msg", length = 256)
    private String descripcionMsg;

    @Column(name = "fh_procesamiento", nullable = false, length = 19)
    private String fhProcesamiento;

    @Column(name = "documento_json", nullable = false, columnDefinition = "TEXT")
    private String documentoJson;

    @Column(name = "observaciones", nullable = false, columnDefinition = "TEXT")
    private String observaciones;

    protected DteEntity() {
    }

    public DteEntity(String codigoGeneracion, String nitEmisor, String tdte, String ambiente, String numeroControl,
                     String selloRecibido, String estado, String clasificaMsg, String codigoMsg,
                     String descripcionMsg, String fhProcesamiento, String documentoJson, String observaciones) {
        this.codigoGeneracion = codigoGeneracion;
        this.nitEmisor = nitEmisor;
        this.tdte = tdte;
        this.ambiente = ambiente;
        this.numeroControl = numeroControl;
        this.selloRecibido = selloRecibido;
        this.estado = estado;
        this.clasificaMsg = clasificaMsg;
        this.codigoMsg = codigoMsg;
        this.descripcionMsg = descripcionMsg;
        this.fhProcesamiento = fhProcesamiento;
        this.documentoJson = documentoJson;
        this.observaciones = observaciones;
    }

    public String getCodigoGeneracion() { return codigoGeneracion; }
    public String getNitEmisor() { return nitEmisor; }
    public String getTdte() { return tdte; }
    public String getAmbiente() { return ambiente; }
    public String getNumeroControl() { return numeroControl; }
    public String getSelloRecibido() { return selloRecibido; }
    public String getEstado() { return estado; }
    public String getClasificaMsg() { return clasificaMsg; }
    public String getCodigoMsg() { return codigoMsg; }
    public String getDescripcionMsg() { return descripcionMsg; }
    public String getFhProcesamiento() { return fhProcesamiento; }
    public String getDocumentoJson() { return documentoJson; }
    public String getObservaciones() { return observaciones; }
}
