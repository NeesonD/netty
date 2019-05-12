package io.netty.example.zneeson.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author : neeson
 * Date: 2019/5/12
 * Time: 17:20
 * Description: 原生 NIO server
 */
public class NioServer {

	private ServerSocketChannel serverSocketChannel;
	private Selector selector;

	private ByteBuffer input = ByteBuffer.allocate(1024);
	private ByteBuffer output = ByteBuffer.allocate(1024);

	private AtomicLong index = new AtomicLong();

	private NioServer(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		// 配置为非阻塞
		serverSocketChannel.configureBlocking(false);
		// bind
		serverSocketChannel.socket().bind(new InetSocketAddress(port));

		selector = Selector.open();
		// 将 channel 注册到 selector
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

		handleKeys();
	}

	private void handleKeys() throws IOException {
		while (!Thread.interrupted()) {
			System.out.println("deep dark");
			// 获取就绪 socket 的个数，这里分阻塞和非阻塞调用，底层可能是用(s elect/poll 或者 epoll 实现的)
			selector.select(60 * 1000);
			System.out.println("循环次数："+(index.incrementAndGet()));
			selector.selectedKeys().forEach(selectionKey -> {
				if (!selectionKey.isValid()) {
					return;
				}
				try {
					handleKey(selectionKey);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	private void handleKey(SelectionKey selectionKey) throws IOException {
		if (selectionKey.isAcceptable()) {
			handleAcceptableKey();
		}
		if (selectionKey.isReadable()) {
			handleReadableKey(selectionKey);
		}
		if (selectionKey.isWritable()) {
			handleWritableKey(selectionKey);
		}
	}

	private void handleWritableKey(SelectionKey selectionKey) throws IOException {
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		// 将响应值放到 buffer
		output.put("success".getBytes());
		// 再从 buffer 写到 channel
		socketChannel.write(output);
		// 清掉缓存
		output.clear();

		socketChannel.register(selector, SelectionKey.OP_READ);
	}

	private void handleReadableKey(SelectionKey selectionKey) throws IOException {
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		// 从 channel 中读出数据到 buffer
		socketChannel.read(input);
		// 业务处理
		process(Arrays.toString(input.array()));
		// 清掉缓存
		input.clear();
		// 将 channel 绑定为 写事件
		socketChannel.register(selector, SelectionKey.OP_WRITE);

	}

	private void handleAcceptableKey() throws IOException {
		// 获取客户端的 socketChannel
		SocketChannel socketChannel = serverSocketChannel.accept();
		if (socketChannel == null) {
			return;
		}
		socketChannel.configureBlocking(false);
		// 注册到socketChannel，并绑定到读事件
		socketChannel.register(selector, SelectionKey.OP_READ);
	}

	private void process(String message) {
		System.out.println("===="+message);
	}

	public static void main(String[] args) throws IOException {
		 new NioServer(9000);
	}

}
