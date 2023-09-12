package maze;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import javax.print.DocFlavor.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Maze extends JFrame {
    private Map<String, String> graph = new HashMap<>();
    private int[][] cellValues; // Matriz para armazenar os valores associados às células

    public void readGraphFromFile(String filePath) {
        int vertexCount = 0;
        int edgeCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    // Se a linha contém dois vértices, conte como uma aresta
                    edgeCount++;
                } else if (parts.length == 3) {
                    // Se a linha contém três partes, conte como uma aresta e um valor
                    edgeCount++;
                }
                vertexCount += parts.length; // Cada parte é um vértice
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Número de vértices: " + vertexCount);
        System.out.println("Número de arestas: " + edgeCount);
    }

    // Método para garantir que a matriz de valores tenha espaço suficiente
    private void ensureCellValueMatrixSize(int size) {
        if (cellValues == null || cellValues.length < size) {
            int[][] newMatrix = new int[size][size];
            if (cellValues != null) {
                for (int i = 0; i < cellValues.length; i++) {
                    System.arraycopy(cellValues[i], 0, newMatrix[i], 0, cellValues[i].length);
                }
            }
            cellValues = newMatrix;
        }
    }

    //pôe números para toda cor que será usada, cores são definidas depois (linha:349 in paint)
    //parades são os blocos pretos
    final static int X = 1;

    //caminhos são os blocos brancos
    final static int C = 0;

    //estado inicial
    final static int S = 2;

    //objetivo
    final static int E = 8;

    //caminho
    final static int V = 9;

    //estado inicial (i,j)
    final static int START_I = 1, START_J = 1;

    //objetivo (i,j)
    final static int END_I = 2, END_J = 9;

    int[][] maze = new int[][]{ // array inicial do labirinto
            //Aqui é possível mudar os valores da matriz afim de modificar o labirinto
            // (1) Paredes
            // (2) Início
            // (0) Caminho
            // (8) Saída
            // (9) É apenas para representação gráfica do caminho que foi explorado (cor verde)
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 2, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 1, 0, 1, 1, 0, 8},
            {1, 0, 1, 1, 1, 0, 1, 1, 0, 1},
            {1, 0, 0, 0, 0, 1, 1, 1, 0, 1},
            {1, 1, 1, 1, 0, 1, 1, 1, 0, 1},
            {1, 1, 1, 1, 0, 1, 0, 0, 0, 1},
            {1, 1, 0, 1, 0, 1, 1, 0, 0, 1},
            {1, 1, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1, 0, 1, 1, 1}
    };

    // para cada array aleatório do qual nós guardamos aleatoriamente um array gerado dentro.
    int[][] arr;

    // Botões da interface (ainda não inicializados)
    JButton solveStack;
    JButton solveBFS;
    JButton clear;
    JButton exit;
    JButton genRandom;

    boolean repaint = false;

    //início
    long startTime;

    //tempo de parada
    long stopTime;

    int[][] savedMaze = clone();

    // o construtor do labirinto, isso será a primeira coisa que será executada quando criar um objeto dessa classe.
    public Maze() {
        setTitle("Labirinto");
        setSize(800, 530);

        java.net.URL urlIcon = getClass().getResource("flat-theme-action-maze-icon.png");
        ImageIcon image = new ImageIcon(urlIcon);
        setIconImage(image.getImage());

        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        // Inicializar objetos para botões
        solveBFS = new JButton("Resolver BFS");
        exit = new JButton("Sair");
        genRandom = new JButton("Gerar Labirinto");

        // Adicionar os botões ao JFrame
        add(solveBFS);
        add(exit);
        add(genRandom);

        // Tornar o JFrame visível (ele é invisível por padrão, não sabemos por quê!)
        setVisible(true);

        // Definir as posições dos componentes no JFrame (x, y, largura, altura).
        // Aqui escolhemos a posição manualmente, por isso configuramos o Layout como nulo.
        solveBFS.setBounds(500, 50, 150, 40);
        genRandom.setBounds(500, 120, 170, 40);
        exit.setBounds(500, 180, 100, 40);

        // O que acontece quando você clica no botão Gerar Labirinto Aleatório
        genRandom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[][] randomMaze = GenerateArray(); // Gere um novo labirinto aleatório
                restore(randomMaze); // Atualize o labirinto com o novo labirinto aleatório
                repaint = true; // Defina repaint como true para redesenhar o labirinto na tela
                repaint(); // Redesenhe a interface gráfica
            }
        });


        // O que acontece quando você clica no botão Sair
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        // O que acontece quando você clica no botão Resolver BFS
        solveBFS.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (arr == null) {
                    restore(savedMaze);
                    repaint = false;
                    solveQueue();
                    repaint();
                } else {
                    restore(arr);
                    repaint = false;
                    solveQueue();
                    repaint();
                }
            }
        });
    }

    // Tamanho do labirinto
    public int Size() {
        return maze.length;
    }

    // Printar labirinto
    public void Print() {
        for (int i = 0; i < Size(); i++) {
            for (int J = 0; J < Size(); J++) {
                System.out.print(maze[i][J]);
                System.out.print(' ');
            }
            System.out.println();
        }
    }

    // retornar verdadeiro se a celula estiver no labirinto
    public boolean isInMaze(int i, int j) {  // parametros são a posição (i and j) da celula

        if (i >= 0 && i < Size() && j >= 0 && j < Size()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isInMaze(MazePos pos) {
        return isInMaze(pos.i(), pos.j());
    }

    // para marcar o nó no array com certo valor; ex: se explorou marque com valro 9 (verde)
    public int mark(int i, int j, int value) {
        assert (isInMaze(i, j));
        int temp = maze[i][j];
        maze[i][j] = value;
        return temp;
    }

    public int mark(MazePos pos, int value) {
        return mark(pos.i(), pos.j(), value);
    }

    // retornar valor se o nó for igual a v = 9(Verde, Explorado)
    public boolean isMarked(int i, int j) {
        assert (isInMaze(i, j));
        return (maze[i][j] == V);

    }

    public boolean isMarked(MazePos pos) {
        return isMarked(pos.i(), pos.j());
    }

    // retornar verdadeiro se o nó é igual 0 (Branco, Inexplorado)
    public boolean isClear(int i, int j) {
        assert (isInMaze(i, j));
        return (maze[i][j] != X && maze[i][j] != V);

    }

    public boolean isClear(MazePos pos) {
        return isClear(pos.i(), pos.j());
    }

    // para ter certeza se chegou na saida (Teste)
    public boolean isFinal(int i, int j) {

        return (i == Maze.END_I && j == Maze.END_J);
    }

    public boolean isFinal(MazePos pos) {
        return isFinal(pos.i(), pos.j());
    }

    // fazer copia do labirinto original
    public int[][] clone() {
        int[][] mazeCopy = new int[Size()][Size()];
        for (int i = 0; i < Size(); i++) {
            for (int j = 0; j < Size(); j++) {
                mazeCopy[i][j] = maze[i][j];
            }
        }
        return mazeCopy;
    }

    // para restaurar o labirinto ao estado inicial
    public void restore(int[][] savedMazed) {
        for (int i = 0; i < Size(); i++) {
            for (int j = 0; j < Size(); j++) {
                maze[i][j] = savedMazed[i][j];
            }
        }

        maze[1][1] = 2;  // ponto inicial
        maze[2][9] = 8;  // objetivo
    }

    //gerar labirinto aleatório com valores de 0 e 1 (blocos preto e branco)
    public int[][] GenerateArray() {
        arr = new int[10][10];
        Random rnd = new Random();
        int min = 0;
        int high = 1;

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int n = rnd.nextInt((high - min) + 1) + min;
                arr[i][j] = n;

            }
        }
        arr[0][1] = 0;arr[1][0] = 0;arr[2][1] = 0;arr[1][2] = 0;
        arr[1][9] = 0;arr[2][8] = 0;arr[3][9] = 0;
        return arr;
    }

    // mostrar o labirinto no JFrame
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.translate(70, 70);      //faça o labirinto começar em 70 para x e 70 para y

        // Desenhar labirinto
        if (repaint == true) {
            for (int row = 0; row < maze.length; row++) {
                for (int col = 0; col < maze[0].length; col++) {
                    Color color;
                    switch (maze[row][col]) {
                        case 1:
                            color = Color.darkGray;       // parede (preto)
                            break;
                        case 8:
                            color = Color.RED;          // objetivo (vermelho)
                            break;
                        case 2:
                            color = Color.YELLOW;      // inicio   (amarelo)
                            break;
                        default:
                            color = Color.WHITE;        // caminho  (branco)
                    }
                    g.setColor(color);
                    g.fillRect(40 * col, 40 * row, 40, 40);    // cobrir retangulo com cor
                    g.setColor(Color.BLUE);
                    g.drawRect(40 * col, 40 * row, 40, 40);    // desenhar retangulo com cor
                }
            }
        }

        if (repaint == false) {   // o que fazer se o repaint for falso (desenhar a solução do labirinto)
            for (int row = 0; row < maze.length; row++) {
                for (int col = 0; col < maze[0].length; col++) {
                    Color color;
                    switch (maze[row][col]) {
                        case 1:
                            color = Color.darkGray;     // parede (preto)
                            break;
                        case 8:
                            color = Color.RED;         // objetivo  (vermelho)
                            break;
                        case 2:
                            color = Color.YELLOW;      // estado inicial   (amarelo)
                            break;
                        case 9:
                            color = Color.green;       // caminho do início até a saída (verde)
                            break;
                        default:
                            color = Color.WHITE;       // caminho livre (branco)
                    }
                    g.setColor(color);
                    g.fillRect(40 * col, 40 * row, 40, 40);  // cobrir retângulo com cor
                    g.setColor(Color.BLUE);
                    g.drawRect(40 * col, 40 * row, 40, 40);  // desenhar retângulo com cor

                }

            }

        }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Maze maze = new Maze();
            }
        });
    }

    public void solveQueue() { //BFS.

        //Iniciar Timer
        startTime = System.nanoTime();

        //criar LinkedList de MazPos (o nó) é o que nós iremos adicionar e remover da LinkedList
        LinkedList<MazePos> list = new LinkedList<MazePos>();

        // adicionar nó inicial a lista.
        list.add(new MazePos(START_I, START_J));

        MazePos crt, next;
        while (!list.isEmpty()) {

            //pegar posição atual
            crt = list.removeFirst();

            //para ter certeza se atingiu o objetivo
            if (isFinal(crt)) { //se o objetivo é alcançado então saia, não precisa explorar mais.
                break;
            }

            //marcar a posição atual como explorada
            mark(crt, V);

            //adicionar nós vizinhos na fila
            next = crt.north();    //cima
            if (isInMaze(next) && isClear(next)) { //isClear() function is used to implement Graph Search
                list.add(next);
            }
            next = crt.east();    //direita
            if (isInMaze(next) && isClear(next)) {
                list.add(next);
            }
            next = crt.west();    //esquerda
            if (isInMaze(next) && isClear(next)) {
                list.add(next);
            }
            next = crt.south();   //baixo
            if (isInMaze(next) && isClear(next)) {
                list.add(next);
            }
        }

        if (!list.isEmpty()) {
            stopTime = System.nanoTime();
            JOptionPane.showMessageDialog(rootPane, "Encontrou a saída!");
        } else {
            JOptionPane.showMessageDialog(rootPane, "Preso no labirinto!!!");
        }

        System.out.println("\nAchar o caminho por algoritmo BFS: ");
        Print();
    }
}