import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static Boolean status;
    private static final Boolean Locked = true;
    private static final Boolean Unlocked = false;
    private static int serverID;
    private static int port;
    private static int compltClientCount = 0;
    private static int msgSentTotal = 0;
    private static int msgRecivedTotal = 0;
    private static final int clientNum = 5;
    ServerSocket ssocket;
    private static ArrayList<Long> tsQueue = new ArrayList<Long>();
    private static HashMap<Integer, Long> idtsMap= new HashMap<Integer, Long>();
    public static HashMap<Integer,Socket> socketMap = new HashMap<Integer,Socket>();
    public static HashMap<Socket,PrintWriter> outs = new HashMap<Socket,PrintWriter>();
    public static HashMap<Socket,BufferedReader> ins = new HashMap<Socket,BufferedReader>();

    public static Random rand = new Random();

    public Server(int serverID, int port) {
        this.serverID = serverID;
        this.port = port;
        status = Unlocked;

        buildConnection();

        //start threads to read sockets
        try {
            while (true) {
                Socket socket = ssocket.accept();
                System.out.println("Server " + serverID + " connect to client@" + socket.getRemoteSocketAddress());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                ins.put(socket,in);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                outs.put(socket, out);
                Thread t = new Thread(new ClientOpHandler(socket));
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
         }
    }

    public class ClientOpHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientOpHandler(Socket  clientSocket) {
            this.clientSocket = clientSocket;
        }

        public synchronized void run() {
            try {
                String inputLine;
                in = ins.get(clientSocket);
                out = outs.get(clientSocket);

                while ((inputLine = in.readLine()) != null) {
                    int cid;
                    Long cts;
                    String outputLine = "";

                    System.out.println((status?"Locked":"Unlocked") + " Msg received: (" + inputLine + ") from client@" + clientSocket.getRemoteSocketAddress());
                    String[] splitStr = inputLine.split("\\s+");

                    if ("REQUEST".equals(splitStr[0])) {
                        msgRecivedTotal++;
                        cid = Integer.parseInt(splitStr[1]);
                        cts = Long.parseLong(splitStr[2]);
                        socketMap.put(cid, clientSocket);

                        if (status == Unlocked) {
                            System.out.println("Send GRANT to client " + cid + clientSocket.getRemoteSocketAddress());
                            outputLine = "GRANT";
                            status = Locked;
                            out.println(outputLine);
                            out.flush();
                            msgSentTotal++;
                        } else {
                            // add to its queue ordered by the timestamp
                            tsQueue.add(cts);
                            idtsMap.put(cid, cts);
                            Collections.sort(tsQueue);
                        }
                    } else if ("RELEASE".equals(splitStr[0])) {
                        msgRecivedTotal++;
                        cid = Integer.parseInt(splitStr[1]);
                        if (tsQueue.isEmpty()) {
                            // no other request
                            status = Unlocked;
                        } else {
                            //dequeue
                            if (idtsMap.containsKey(cid)) {
                                tsQueue.remove(0);
                                idtsMap.remove(cid);
                                if (tsQueue.isEmpty()) {
                                    status = Unlocked;
                                } else {
                                    cid = getKey(idtsMap, tsQueue.get(0));
                                    // send GRANT to next REQ
                                    System.out.println("Send GRANT to client " + cid +  socketMap.get(cid).getRemoteSocketAddress());
                                    out = outs.get(socketMap.get(cid));
                                    out.println("GRANT");
                                    out.flush();
                                    msgSentTotal++;
                                }
                            } else {
                               cid = getKey(idtsMap, tsQueue.get(0));
                               // send GRANT to next REQ
                               System.out.println("Send GRANT to client " + cid +  socketMap.get(cid).getRemoteSocketAddress());
                               out = outs.get(socketMap.get(cid));
                               out.println("GRANT");
                               out.flush();
                               msgSentTotal++;
                            }
                        }
                    } else if ("COMPLETION".equals(splitStr[0])) {
                        if (serverID == 1) {
                            if (++compltClientCount == clientNum) {
                                // brings the entire distributed computation to an end
                                outputLine = "END";
                                for (PrintWriter outf : outs.values()) {
                                    outf.println(outputLine);
                                    outf.flush();
                                }
                            }
                         }
                    } else if ("END".equals(splitStr[0])) {
                        System.out.println("###  total number of messages sent: " + msgSentTotal + ", total number of messages received: " + msgRecivedTotal);
                        stop();
                    } else {
                        outputLine = "UNRCOGNIZED OPERATION";
                        out.println(outputLine);
                        out.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
             }
        }

        public void stop() {
            System.out.println("Server " + serverID + " closing itself");

            try {
                for (BufferedReader in : ins.values()) {
                    in.close();
                }
                for (PrintWriter out : outs.values()) {
                    out.close();
                }
                for (Socket socket : socketMap.values()) {
                    socket.close();
                }
                ssocket.close();
                Thread.sleep(100);
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void buildConnection () {
        try {
            // build data stream
            //System.out.println("Server " + serverID + " starts serverSocket at port: " + port);
            ssocket = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <K, V> K getKey(Map<K, V> map, V value) {
        for (K key : map.keySet()) {
            if (value.equals(map.get(key))) {
                return key;
            }
        }
        return null;
    }

    // need user input of id
    public static void main(String[] args) throws IOException {
        // get id
        if (args.length > 0){
            try{
                new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (NumberFormatException e){
                System.err.println("Argument must be an integer");
                System.exit(1);
            }
        }
    }
}
