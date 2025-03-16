import ws from 'k6/ws';
import { check, sleep } from 'k6';

export let options = {
    stages: [
        { duration: '10s', target: 10000 }, // Tăng dần lên 10,000 client trong 10s
        { duration: '60s', target: 10000 }, // Giữ 10,000 client trong 30s
        { duration: '10s', target: 0 },     // Giảm về 0
    ]
};

export default function () {
    let url = 'ws://localhost:8080/go/ws';

    let params = { headers: { 'X-Client-ID': `client-${__VU}` } };
    let res = ws.connect(url, params, function (socket) {
        socket.on('open', function () {
            console.log(`Client ${__VU} connected`);
            socket.send(JSON.stringify({ payload: 'Hello Server' }));
        });

        socket.on('message', function (data) {
            console.log(`Client ${__VU} received: ${data}`);
        });

        socket.on('close', function () {
            console.log(`Client ${__VU} disconnected`);
        });

        socket.on('error', function (e) {
            console.log(`Client ${__VU} error: ${e.error()}`);
        });

        // Gửi 10 tin nhắn, mỗi 100ms gửi 1 tin nhắn
        for (let i = 0; i < 10; i++) {
            socket.send(JSON.stringify({ payload: `Msg ${i}` }));
            sleep(0.1); // 100ms
        }

        sleep(10); // Giữ kết nối trong 10 giây trước khi đóng
    });

    check(res, { 'WebSocket connection successful': (r) => r && r.status === 101 });
}


// k6 run go-server/websocket/test/loadtest.js
//prometheus --config.file=/usr/local/etc/prometheus.yml http://localhost:9090
//brew services start grafana http://localhost:3000

