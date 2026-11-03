package br.com.autopro.model;

import java.time.LocalDate;

/** Filtros opcionais aplicáveis aos relatórios. Campos nulos/vazios são ignorados. */
public class FiltroRelatorio {

    private String cliente;
    private String placa;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private StatusOrdem status;

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public StatusOrdem getStatus() { return status; }
    public void setStatus(StatusOrdem status) { this.status = status; }
}
