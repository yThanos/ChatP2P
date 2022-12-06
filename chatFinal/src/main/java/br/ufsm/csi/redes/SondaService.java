package br.ufsm.csi.redes;

import br.ufsm.csi.redes.Model.Sonda;
import br.ufsm.csi.redes.Model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SondaService {
    public static final Set<Usuario> listaUsuarios = new HashSet<>();//lista dos usuarios encontrados pela sonda
    private ChatClientSwing chat;//instancia que recebe o chatclientswing

    public class EnviaSonda implements Runnable{

        @SneakyThrows
        @Override
        public void run() {
            DatagramSocket datagramSocket = new DatagramSocket();//abre um datagramsocket UDP
            while (true){
                Sonda sonda = Sonda.builder().tipoMensagem("sonda").usuario(chat.meuUsuario.getNome()).status(chat.meuUsuario.getStatus()).build();//cria um objeto Sonda com o Usuario q vem do ChatClientSwing
                byte[] pacote = new ObjectMapper().writeValueAsString(sonda).getBytes(StandardCharsets.UTF_8);//transforma a sonda em String e pega os bytes para enviar
                DatagramPacket packet = new DatagramPacket(pacote, 0, pacote.length, InetAddress.getByName("255.255.255.255"), 8080);//pega o pacote em bytes, sem offset, com o tamanho do pacote e o endereço de streaming e monta um packet
                datagramSocket.send(packet);//envia o pacote
                Thread.sleep(5000);//dorme por 5 segundos
            }
        }
    }

    public class RecebeSonda implements Runnable{

        @SneakyThrows
        @Override
        public void run() {
            DatagramSocket datagramSocket = new DatagramSocket(8080);//abre um datagramsocket que escuta a porta 8080
            while (true){
                byte[] buffer = new byte[1024];//define um buffer com espaço mais que o suficiente para receber a sonda
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);//cria um pacote com o tamanho do buffer
                datagramSocket.receive(packet);//recebe o pacote que estiver na porta 8080
                String pacote = new String(buffer, 0, packet.getLength(), StandardCharsets.UTF_8);//pega o que tiver dentro do pacote e guarda dentro de uma string
                Sonda sonda = new ObjectMapper().readValue(pacote, Sonda.class);//converte a string para um Objeto Sonda
                Usuario usuario = Usuario.builder().nome(sonda.getUsuario()).lifespan(System.currentTimeMillis()).status(sonda.getStatus()).endereco(packet.getAddress()).build();//cria um novo Usuario igual o usuario que veio na sonda
                //if(!packet.getAddress().toString().equals("/"+InetAddress.getLocalHost().getHostAddress())) {//não adiciona na lista se for uma sonda recebida de si mesmo
                    synchronized (listaUsuarios) {//sincroniza com a lista de usuarios
                        listaUsuarios.remove(usuario);//remove o usuario para caso ele ja estivesse na lista
                        listaUsuarios.add(usuario);//adiciona o usuario para manter o lifespan atualizado
                    }
                //}
                System.out.println(listaUsuarios);//mostra a  lista no terminal
            }
        }
    }

    public class AtualizaLista implements Runnable {

        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                Thread.sleep(8000);//dorme por 8 segundos
                synchronized (listaUsuarios) {//sincroniza com a lista de usuarios
                    List<Usuario> listaRemover = listaUsuarios.stream().filter(u -> (System.currentTimeMillis() - u.getLifespan()) > 30000).toList();//cria uma lista com os usuarios que não enviaram mais sondas
                    listaRemover.stream().forEach(u -> chat.dfListModel.removeElement(u));//remove os usuarios ausentes
                }
                listaUsuarios.stream().forEach(u ->  chat.dfListModel.removeElement(u));//remove o usuario para caso ele ja estivesse na lista
                listaUsuarios.stream().forEach(u ->  chat.dfListModel.addElement(u));//adiciona o usuario para manter o lifespan atualizado
            }
        }
    }

    public void init(){
        new Thread(new RecebeSonda()).start();//da inicio a thread que fica recebendo as sondas
        new Thread(new EnviaSonda()).start();//da inicio a thread que fica enviando as sondas
        new Thread(new AtualizaLista()).start();//da inicio a thread que fica removendo os usuarios ausentes
    }

    public SondaService(ChatClientSwing chat){
        this.chat = chat;//recebe como parametro a classe chatswing e instancia ela
        init();//chama o metodo que inicia as threads
    }
}