package br.ufsm.csi.redes.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mensagem {
    private String mensagem;
    private Usuario user;
}
