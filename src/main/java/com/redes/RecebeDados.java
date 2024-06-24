/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redes;

/**
 * @author flavio
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecebeDados extends Thread {

    private final int portaLocalReceber = 2001;
    private final int portaLocalEnviar = 2002;
    private final int portaDestino = 2003;
    private int ack_recebido = 0;

    private void enviaAck(int ack) {

        try {
            InetAddress address = InetAddress.getByName("localhost");
            try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnviar)) {
                byte[] sendData = new byte[4];
                sendData[0] = (byte) ((ack >> 24) & 0xFF);
                sendData[1] = (byte) ((ack >> 16) & 0xFF);
                sendData[2] = (byte) ((ack >> 8) & 0xFF);
                sendData[3] = (byte) (ack & 0xFF);


                DatagramPacket packet = new DatagramPacket(
                        sendData, sendData.length, address, portaDestino);

                datagramSocket.send(packet);
            }
        } catch (SocketException ex) {
            Logger.getLogger(RecebeDados.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RecebeDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(portaLocalReceber);
            byte[] receiveData = new byte[1400];
            try (FileOutputStream fileOutput = new FileOutputStream("saida")) {
                boolean fim = false;
                while (!fim) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    System.out.println("pacote: " + ack_recebido + " recebido");

                    byte[] tmp = receivePacket.getData();

                    //probabilidade de 60% de perder
                    //gero um numero aleatorio contido entre [0,1]
                    //se numero cair no intervalo [0, 0,6)
                    //significa perda, logo, você não envia ACK
                    //para esse pacote, e não escreve ele no arquivo saida.
                    //se o numero cair no intervalo [0,6, 1,0]
                    //assume-se o recebimento com sucesso.

                    for (int i = 0; i < tmp.length; i = i + 4) {
                        int dados = ((tmp[i] & 0xff) << 24) + ((tmp[i + 1] & 0xff) << 16) + ((tmp[i + 2] & 0xff) << 8) + ((tmp[i + 3] & 0xff));

                        if (dados == -1) {
                            ack_recebido = -1;
                        }
                        fileOutput.write(dados);

                    }
                    enviaAck(ack_recebido);
                    ack_recebido++;
                }
            }
        } catch (IOException e) {
            System.out.println("Excecao: " + e.getMessage());
        }
    }
}
