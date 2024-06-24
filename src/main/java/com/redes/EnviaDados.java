
package com.redes;

/**
 * @author flavio
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Semaphore;

public class EnviaDados extends Thread {

    private final int portaLocalEnvio = 2000;
    private final int portaDestino = 2001;
    private final int portaLocalRecebimento = 2003;
    private final int TIMEOUT = 75; // tempo em milissegundos para timeout
    private int ultimo_pkt_enviado = 0;
    private int ack_esperado = 0;
    private Timer timer = new Timer();
    Semaphore sem;
    private final String funcao;
    private TimerTask timerTask;

    public EnviaDados(Semaphore sem, String funcao) {
        super(funcao);
        this.sem = sem;
        this.funcao = funcao;
    }

    public String getFuncao() {
        return funcao;
    }

    private void enviaPct(int[] dados) {
        //converte int[] para byte[]
        ByteBuffer byteBuffer = ByteBuffer.allocate(dados.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(dados);

        byte[] buffer = byteBuffer.array();

        try {
            System.out.println("Semaforo: " + sem.availablePermits());
            sem.acquire();
            System.out.println("Semaforo: " + sem.availablePermits());

            InetAddress address = InetAddress.getByName("localhost");
            try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnvio)) {
                DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length, address, portaDestino);
                datagramSocket.send(packet);
            }
            System.out.println("Envio do pkt " + ultimo_pkt_enviado + " feito.");
            ultimo_pkt_enviado ++;
        } catch (SocketException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void iniciarTimer() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        timerTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Timeout! Retransmitindo pacote..." + ack_esperado);
                ultimo_pkt_enviado = ack_esperado;
            }
        };

        timer.schedule(timerTask, TIMEOUT);
    }

    private void processaAck(int ack) {
        if (ack == ack_esperado || ack == -1) {
            ack_esperado++;
            if (timerTask != null) {
                timerTask.cancel(); // Cancela o timer atual
            }
            timerTask = null; // Reinicia a referência do timerTask
        }
    }

    @Override
    public void run() {
        switch (this.getFuncao()) {
            case "envia":
                //variavel onde os dados lidos serao gravados
                int[] dados = new int[350];
                int[][] buffer_dados = new int[4][350];
                //contador, para gerar pacotes com 1400 Bytes de tamanho
                //como cada int ocupa 4 Bytes, estamos lendo blocos com 350
                //int's por vez.
                int cont = 0;

                try (FileInputStream fileInput = new FileInputStream("entrada");) {
                    int lido;
                    while ((lido = fileInput.read()) != -1) {
                        buffer_dados[ultimo_pkt_enviado%4][cont] = lido;
                        cont++;
                        if (cont == 350) {
                            //envia pacotes a cada 350 int's lidos.
                            //ou seja, 1400 Bytes.
                            dados = buffer_dados[ultimo_pkt_enviado % 4];
                            enviaPct(dados);
                            iniciarTimer();
                            cont = 0;
                        }

                    }

                    //ultimo pacote eh preenchido com
                    //-1 ate o fim, indicando que acabou
                    //o envio dos dados.
                    for (int i = cont; i < 350; i++)
                        dados[i] = -1;
                    enviaPct(dados);
                    iniciarTimer();
                } catch (IOException e) {
                    System.out.println("Error message: " + e.getMessage());
                }
                break;
            case "ack":
                try {
                    DatagramSocket serverSocket = new DatagramSocket(portaLocalRecebimento);
                    byte[] receiveData = new byte[4];
                    int ack = 0;
                    while (true) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);
                        ByteBuffer wrapped = ByteBuffer.wrap(receivePacket.getData());
                        ack = wrapped.getInt();
                        System.out.println("Ack recebido " + ack + ".");
                        processaAck(ack);
                        sem.release();
                        if(ack == -1){
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Excecao: " + e.getMessage());
                }
                break;
            //TODO timer
            default:
                break;
        }

    }
}