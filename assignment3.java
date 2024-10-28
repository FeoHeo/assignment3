import java.io.*;
import java.net.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class assignment3 {
    private static String WORD = "happy";
    private static int PORT = 12345;
    private static final int BUFFER_SIZE = 1024;
    private static final int MAX_CONNECTIONS = 10;
    private static final String FILENAME_FORMAT = "book_%2d.txt";
    private static final String FILENAME_FORMAT_ALT = "book_%02d.txt";

    static class ListNode {
        String line;
        ListNode nextNode;       // Link to the next element in the shared list
        ListNode bookNextNode;   // Link to the next item in the same book
        int bookNum;            // The id used to track book

        ListNode(String line , int number) {
            this.line = line;
            this.bookNum = number;
        };
    }

    static class SharedList {
        ListNode head;             // Head of the linked list for each book
        Lock lock;                 // Lock for thread-safe access
        int bookTotal;           // Track the amount of book

        SharedList() {
            this.head = null;
            this.bookTotal = 1;
            this.lock = new ReentrantLock();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final SharedList sharedList;

        ClientHandler(Socket clientSocket, SharedList sharedList) {
            this.clientSocket = clientSocket;
            this.sharedList = sharedList;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String line;
                int bookNumber;

                sharedList.lock.lock();
                try {
                    bookNumber = sharedList.bookTotal++;
                } finally {
                    sharedList.lock.unlock();
                }

                // Read data from the client
                while ((line = reader.readLine()) != null) {
                    addToList(sharedList, line, bookNumber);
                }
                
                
                // Write to file
                String filename;
                if(bookNumber >= 10) {
                    filename = String.format(FILENAME_FORMAT, bookNumber);
                } else {
                    filename = String.format(FILENAME_FORMAT_ALT, bookNumber);
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {

                    ListNode currentNode = sharedList.head;
                    while (currentNode != null) {
                        if(currentNode.bookNum == bookNumber) {
                            writer.write(currentNode.line + "   $" + currentNode.bookNum);
                            writer.newLine();
                        }
                        currentNode = currentNode.nextNode;
                    }
                    
                }
                
                // Write out the list
                try(BufferedWriter wrt = new BufferedWriter(new FileWriter("Total.txt"))) {
                    ListNode curr = sharedList.head;
                    while(curr != null) {
                        wrt.write(curr.line + "  $" + curr.bookNum);
                        wrt.newLine();
                        curr = curr.nextNode;
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }
                
                
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    
    private static void addToList(SharedList sharedList, String line, int bookNumber) {
        sharedList.lock.lock();
        try {
            ListNode newNode = new ListNode(line , bookNumber);
            
            if (sharedList.head == null) {
                sharedList.head = newNode;  // If it's the first node, set as head
            } else {
                ListNode currentNode = sharedList.head;
                while (currentNode.nextNode != null) {
                    currentNode = currentNode.nextNode;
                }
                currentNode.nextNode = newNode; // Add to the end of the list

                currentNode = sharedList.head;
                while (currentNode.bookNextNode != null) {   // Iterate to where bookNextNode is null
                    System.out.println("bookNextNode adress: " + currentNode.bookNextNode.toString());
                    System.err.println("currNode content: " + currentNode.line);
                    currentNode = currentNode.bookNextNode;
                }
                if (currentNode.bookNum == bookNumber) { // Check if it belongs to the same book
                    currentNode.bookNextNode = newNode; // Link to the book's nodes
                }
            }
            
            // Update bookNextNode pointers
            //if (bookNumber > 1) {
            //}


                
            
            System.out.println("Added: " + line);
        } finally {
            sharedList.lock.unlock();
        }
    }


    public static void startServer(SharedList sharedList) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Incoming connection from: " + clientSocket.getPort());
                new Thread(new ClientHandler(clientSocket, sharedList)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        for(int i=0 ; i<args.length; i++) {
            if(args[i].equals("-l")) {
                try {
                    PORT = Integer.parseInt(args[i+1]);
                } catch (Exception e) {
                    System.err.println("No port provided, using default port");
                }
            }

            if(args[i].equals("-s")) {
                try {
                    WORD = args[i+1];
                } catch (Exception e) {
                    System.err.println("No search string provided, using default search " + WORD);
                }
            }

        }
        

        SharedList sharedList = new SharedList();
        startServer(sharedList);
    }
}
