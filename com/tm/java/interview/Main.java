package com.tm.java.interview;

import java.util.LinkedList;
import java.util.Queue;

public class Main {
    public static void main(String[] args) {

        char[][] input = {
                {'1', '1', '0', '0', '0'},
                {'1', '1', '0', '0', '1'},
                {'0', '0', '1', '0', '1'},
                {'0', '0', '1', '1', '1'}
        };

        System.out.println("DFS=" + numIslandsDfs(input));
        System.out.println("BFS=" + numIslandsBfs(input));
    }

    public static int numIslandsDfs(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;

        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        int count = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == '1' && !visited[r][c]) {
                    dfs(grid, visited, r, c);
                    count++;
                }
            }
        }

        return count;
    }

    public static int numIslandsBfs(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;

        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        int count = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == '1' && !visited[r][c]) {
                    bfs(grid, visited, r, c);
                    count++;
                }
            }
        }

        return count;
    }

    private static void dfs(char[][] grid, boolean[][] visited, int r, int c) {
        int rows = grid.length;
        int cols = grid[0].length;

        if (r < 0 || c < 0 || r >= rows || c >= cols
                || grid[r][c] == '0' || visited[r][c]) {
            return;
        }

        visited[r][c] = true;

        dfs(grid, visited, r - 1, c);
        dfs(grid, visited, r + 1, c);
        dfs(grid, visited, r, c - 1);
        dfs(grid, visited, r, c + 1);
    }

    private static void bfs(char[][] grid, boolean[][] visited, int startR, int startC) {
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{startR, startC});
        visited[startR][startC] = true;

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int r = cell[0];
            int c = cell[1];

            for (int i = 0; i < 4; i++) {
                int newR = r + dr[i];
                int newC = c + dc[i];

                if (isValid(grid, visited, newR, newC)) {
                    queue.offer(new int[]{newR, newC});
                    visited[newR][newC] = true;
                }
            }
        }
    }

    private static boolean isValid(char[][] grid, boolean[][] visited, int r, int c) {
        int rows = grid.length;
        int cols = grid[0].length;
        return r >= 0 && c >= 0 && r < rows && c < cols &&
                grid[r][c] == '1' && !visited[r][c];
    }
}
