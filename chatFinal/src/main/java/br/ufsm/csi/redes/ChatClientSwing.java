package br.ufsm.csi.redes;

import br.ufsm.csi.redes.Model.Mensagem;
import br.ufsm.csi.redes.Model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static br.ufsm.csi.redes.Model.Usuario.Status.*;

/**
 * 
 * User: Vitor e Bianca
 * Date: 05/12/22
 * Time: 19:59
 * 
 */
public class ChatClientSwing extends JFrame {

    public static Usuario meuUsuario;//salva o meu usuario
    private JList listaChat;//não sei
    public DefaultListModel dfListModel;//salva a lista com os usuarios do chat
    private JTabbedPane tabbedPane = new JTabbedPane();//não sei
    private Set<Usuario> chatsAbertos = new HashSet<>();//não sei
    private Socket socket;//define um socket que sera para quem sera enviada a mensagem
    private PainelChatPVT painel;//painel do EU

    @SneakyThrows
    public ChatClientSwing() throws UnknownHostException {
        setLayout(new GridBagLayout());
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Status");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(DISPONIVEL.name());
        rbMenuItem.setSelected(true);
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(DISPONIVEL);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(NAO_PERTURBE.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(NAO_PERTURBE);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(VOLTO_LOGO.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(VOLTO_LOGO);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        menuBar.add(menu);
        this.setJMenuBar(menuBar);

        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popupMenu =  new JPopupMenu();
                    final int tab = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
                    JMenuItem item = new JMenuItem("Fechar");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PainelChatPVT painel = (PainelChatPVT) tabbedPane.getTabComponentAt(tab);
                            tabbedPane.remove(tab);
                            chatsAbertos.remove(painel.getUsuario());
                        }
                    });
                    popupMenu.add(item);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        add(tabbedPane, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        setSize(800, 600);
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width - this.getWidth()) / 2;
        final int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Chat P2P - Redes de Computadores");
        String nomeUsuario = JOptionPane.showInputDialog(this, "Digite seu nome de usuario: ");//lê o nome digitado
        this.meuUsuario = new Usuario(nomeUsuario, DISPONIVEL, InetAddress.getLocalHost());//define um usuario com o nome escolhido e com o meu endereço
        new SondaService(this);//chama a classe SondaService passando o nome escolhido
        new Thread(new Sessao()).start();//da inicio a thread que abre sessao
        Thread.sleep(5000);//dorme por 5 segundos para evitar abrir sem a lista carregada
        add(new JScrollPane(criaLista()), new GridBagConstraints(0, 0, 1, 1, 0.1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));//chama o metodo criaLista colocando os usuarios que a Sonda pega na tela
        this.painel = new PainelChatPVT(meuUsuario, "eu");//define o painel do usuario EU
        setVisible(true);
    }

    private JComponent criaLista() {
        dfListModel = new DefaultListModel();

        SondaService.listaUsuarios.stream().forEach(usuario -> dfListModel.addElement(usuario));//preenche a lista com os usuarios pegos pela Sonda

        listaChat = new JList(dfListModel);
        listaChat.addMouseListener(new MouseAdapter() {
            @SneakyThrows
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    Usuario user = (Usuario) list.getModel().getElementAt(index);
                    socket = new Socket(user.getEndereco(), 8081);//abre um socket com o endereço do usuarios selecionado na lsita
                    if (chatsAbertos.add(user)) {
                        //PainelChatPVT painel2 = new PainelChatPVT(user, "outro eu");
                        tabbedPane.add(user.toString(), painel);//abre uma janela de chat com o usuario
                    }
                }
            }
        });
        return listaChat;
    }


    class PainelChatPVT extends JPanel {

        JTextArea areaChat;
        JTextField campoEntrada;
        Usuario usuario;
        String ponteiro;

        PainelChatPVT(Usuario usuario, String ponteiro) {
            this.ponteiro = ponteiro;
            setLayout(new GridBagLayout());
            areaChat = new JTextArea();
            this.usuario = usuario;
            areaChat.setEditable(false);
            campoEntrada = new JTextField();
            campoEntrada.addActionListener(new ActionListener() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((JTextField) e.getSource()).setText("");
                    painel.areaChat.append(meuUsuario.getNome() + "> " + e.getActionCommand() + "\n");
                    Mensagem msg = Mensagem.builder().mensagem(e.getActionCommand()).user(meuUsuario).build();//cria um Objeto Mensagem com a mensagem escrita e o usuario que escreveu(no caso eu)
                    String strMsg = new ObjectMapper().writeValueAsString(msg);//transforma o Obejto Mensagem em string
                    socket.getOutputStream().write(strMsg.getBytes(StandardCharsets.UTF_8));//envia para o socket que foi aberto no metodo criaLista
                }
            });
            add(new JScrollPane(areaChat), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(campoEntrada, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        }
        public Usuario getUsuario() {
            return usuario;
        }
    }

    public class Sessao implements Runnable {
        private ServerSocket serverSocket;//define um serverSocket

        @SneakyThrows
        public Sessao(){
            this.serverSocket = new ServerSocket(8081);//deixa o serversocket escutando a porta 8081
        }

        @SneakyThrows
        @Override
        public void run() {
            Socket soc = this.serverSocket.accept();
            byte[] buffer = new byte[1024];//define um buffer capaz de armazenar a Mensagem
            int size = soc.getInputStream().read(buffer);//recebe a mensagem e coloca no buffer
            String msg = new String(buffer, 0, size);//le o buffer e salva a Mensagem em uma string
            Mensagem mensagem = new ObjectMapper().readValue(msg, Mensagem.class);//transforma a string em um Objeto Mensagem
            Usuario user = mensagem.getUser();//cria um usuario usando o usuario que enviou a mensagem
            PainelChatPVT painel2 = new PainelChatPVT(user, "outro eu");//define o painel do usuario OUTRO EU
            tabbedPane.add(user.toString(), painel2);//abre uma janela de chat com o usuario que enviou a mensagem
            painel2.areaChat.append(user.getNome()+"> "+ mensagem.getMensagem()+"\n");//coloca na tela a mensagem e o nome do usuario
            new Thread(new RecebeMsg(soc, painel2)).start();//inicia a classe que recebe as mensagens
        }

        public class RecebeMsg implements Runnable{
            private Socket socket;//define um socket que sera preenchido com o socket do usuario para quem enviamos a mensagem
            PainelChatPVT painel;//instancia que recebe um painel

            public RecebeMsg(Socket socket, PainelChatPVT painel){
                this.socket = socket;//preenche o socket com o socket do serversocket
                this.painel = painel;//pega o painel do OUTRO EU e usa como o painel
            }

            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    byte[] buffer = new byte[1024];//define um buffer capaz de armazenar a Mensagem
                    int size = socket.getInputStream().read(buffer);//recebe a mensagem e coloca no buffer
                    String msg = new String(buffer, 0, size);//le o buffer e salva a Mensagem em uma string
                    Mensagem mensagem = new ObjectMapper().readValue(msg, Mensagem.class);//transforma a string em um Objeto Mensagem
                    Usuario user = mensagem.getUser();//cria um usuario usando o usuario que enviou a mensagem
                    painel.areaChat.append(user.getNome() + "> " + mensagem.getMensagem() + "\n");//coloca na tela a mensagem e o nome do usuario
                }
            }
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        new ChatClientSwing();//executavel que inicia tudo
    }
}