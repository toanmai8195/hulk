# Ví dụ LangChain với Bazel

Thư mục này chứa các ứng dụng LangChain mẫu được xây dựng với Bazel và Python, sử dụng Groq làm LLM provider.

## 📁 Cấu trúc Project

```
com/tm/python/langchain/
├── BUILD                  # Cấu hình build Bazel
├── requirements.txt       # Dependencies Python
├── README.md              # File này
├── .env.template          # Template biến môi trường
├── simple_chain.py        # Ví dụ LangChain chain cơ bản với Groq
├── chat_example.py        # AI hội thoại có memory
├── demo.py                # Demo tổng hợp
└── rag_example.py         # Ví dụ Retrieval Augmented Generation
```

## 🚀 Các Ví dụ

### 1. Simple Chain (`simple_chain.py`)
- LangChain chain cơ bản với prompt templates và Groq
- Tạo các thông tin thú vị về nhiều chủ đề
- Minh họa các khái niệm cốt lõi của LangChain

### 2. Chat Example (`chat_example.py`) 
- Chatbot tương tác có bộ nhớ hội thoại sử dụng LangChain + Groq
- Duy trì ngữ cảnh qua nhiều cuộc trao đổi
- Cách xây dựng ứng dụng AI hội thoại

### 3. Demo (`demo.py`)
- Demo tổng quan về các khái niệm LangChain
- Minh họa cấu trúc prompts, chains, memory, và RAG concepts
- Chạy được mà không cần API key

### 4. RAG Example (`rag_example.py`)
- Hệ thống hỏi đáp dựa trên tài liệu với LangChain + Groq
- Sử dụng HuggingFace embeddings và vector search
- Minh họa Retrieval Augmented Generation hoàn chỉnh

## 🛠️ Cài đặt

Project đã sẵn sàng sử dụng! Các dependencies cần thiết đã được cài đặt trong virtual environment.

### 1. Cấu hình Groq

Đối với các ví dụ sử dụng LangChain với Groq:

```bash
# Lấy API key miễn phí tại https://console.groq.com/
# Copy .env template và thêm API key
cp .env.template .env
# Sửa .env file và thêm GROQ_API_KEY của bạn
```

### 2. Build với Bazel

```bash
# Build tất cả ví dụ
bazel build //com/tm/python/langchain/...

# Hoặc build từng ví dụ cụ thể
bazel build //com/tm/python/langchain:simple_chain
bazel build //com/tm/python/langchain:chat_example  
bazel build //com/tm/python/langchain:demo
bazel build //com/tm/python/langchain:rag_example
```

### 3. Chạy Ví dụ

**Tùy chọn A: Với Bazel (Khuyến nghị)**
```bash
# Chạy ví dụ simple chain
bazel run //com/tm/python/langchain:simple_chain

# Chạy ví dụ chat tương tác
bazel run //com/tm/python/langchain:chat_example

# Chạy demo (không cần API key)
bazel run //com/tm/python/langchain:demo

# Chạy ví dụ RAG Q&A
bazel run //com/tm/python/langchain:rag_example
```

**Tùy chọn B: Chạy Python trực tiếp (Khuyến nghị cho development)**
```bash
# Cài đặt dependencies
cd com/tm/python/langchain
pip install -r requirements.txt

# Copy và cấu hình .env file  
cp .env.template .env
# Sửa .env và thêm GROQ_API_KEY của bạn

# Chạy các ví dụ
python3 simple_chain.py
python3 chat_example.py
python3 demo.py  # không cần API key
python3 rag_example.py
```

## 📦 Dependencies

Project sử dụng các package LangChain sau:

- `langchain` - Framework cốt lõi
- `langchain-community` - Tích hợp cộng đồng 
- `langchain-groq` - Tích hợp Groq
- `langchain-huggingface` - Tích hợp HuggingFace embeddings
- `python-dotenv` - Biến môi trường

## 🔧 Phát triển

### Thêm Ví dụ Mới

1. Tạo file Python mới với ví dụ LangChain của bạn
2. Thêm file vào `BUILD`:

```python
py_binary(
    name = "my_example",
    srcs = ["my_example.py"],
    deps = [":langchain_lib"],
    main = "my_example.py",
)
```

3. Build và chạy:

```bash
bazel run //com/tm/python/langchain:my_example
```

### Cập nhật Dependencies

1. Cập nhật `requirements.txt` với packages mới
2. Cấu hình Bazel trong `MODULE.bazel` sẽ tự động sử dụng requirements mới

## 🚨 Lưu ý Quan trọng

- **Cần Groq API Key**: Hầu hết ví dụ cần GROQ_API_KEY trong file .env (trừ demo.py)
- **Bazel vs Python**: Bazel build chỉ cho structure, cần chạy Python trực tiếp để có dependencies
- **Cloud Processing**: Xử lý thông qua Groq API, cần kết nối internet
- **Hiệu năng**: Groq cung cấp tốc độ inference cực nhanh
- **Rate Limits**: Free tier có giới hạn số requests, cần theo dõi usage

## 🎯 Trường hợp Sử dụng

Các ví dụ này minh họa các pattern LangChain cho:

- ✅ Kỹ thuật prompt engineering và chains cơ bản
- ✅ Ứng dụng AI hội thoại  
- ✅ Hệ thống hỏi đáp tài liệu
- ✅ Tìm kiếm vector tương đồng
- ✅ Quản lý bộ nhớ trong hội thoại
- ✅ Tích hợp với Groq API

## 🔗 Liên kết Hữu ích

- [Tài liệu LangChain](https://python.langchain.com/)
- [Groq Console](https://console.groq.com/)
- [LangChain Groq Integration](https://python.langchain.com/docs/integrations/chat/groq)
- [Bazel Python Rules](https://github.com/bazelbuild/rules_python)