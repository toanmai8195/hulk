load("@rules_python//python:defs.bzl", "py_binary")

py_binary(
    name = "simple_chain",
    srcs = ["simple_chain.py"],
    main = "simple_chain.py",
    deps = [
        "@pip//langchain",
        "@pip//langchain_groq", 
        "@pip//python_dotenv",
    ],
)

py_binary(
    name = "chat_example",
    srcs = ["chat_example.py"],
    main = "chat_example.py",
    deps = [
        "@pip//langchain",
        "@pip//langchain_groq",
        "@pip//python_dotenv",
    ],
)

py_binary(
    name = "rag_example", 
    srcs = ["rag_example.py"],
    main = "rag_example.py",
    deps = [
        "@pip//langchain",
        "@pip//langchain_community",
        "@pip//langchain_groq",
        "@pip//langchain_huggingface",
        "@pip//python_dotenv",
        "@pip//sentence_transformers",
        "@pip//faiss_cpu",
    ],
)

py_binary(
    name = "demo",
    srcs = ["demo.py"],
    main = "demo.py",
    deps = [
        "@pip//langchain",
    ],
)

py_binary(
    name = "spam_detector_rag",
    srcs = ["spam_detector_rag.py"],
    main = "spam_detector_rag.py",
    deps = [
        "@pip//langchain",
        "@pip//langchain_community",
        "@pip//langchain_groq",
        "@pip//langchain_huggingface",
        "@pip//python_dotenv",
        "@pip//sentence_transformers",
        "@pip//faiss_cpu",
    ],
)

py_binary(
    name = "hbase_data_generator",
    srcs = ["hbase_data_generator.py"],
    main = "hbase_data_generator.py",
    deps = [
        "@pip//happybase",
    ],
)

py_binary(
    name = "spam_detector_hbase",
    srcs = ["spam_detector_hbase.py"],
    main = "spam_detector_hbase.py",
    deps = [
        "@pip//langchain",
        "@pip//langchain_community",
        "@pip//langchain_groq",
        "@pip//langchain_huggingface",
        "@pip//python_dotenv",
        "@pip//sentence_transformers",
        "@pip//faiss_cpu",
        "@pip//happybase",
        "@pip//requests",
    ],
)

py_binary(
    name = "run_spam_detection",
    srcs = ["run_spam_detection.py"],
    main = "run_spam_detection.py",
    data = [
        ":hbase_data_generator.py",
        ":spam_detector_hbase.py",
        ".env",
    ],
)

py_binary(
    name = "test_hbase_workflow",
    srcs = ["test_hbase_workflow.py"],
    main = "test_hbase_workflow.py",
    deps = [
        "@pip//langchain",
        "@pip//langchain_community",
        "@pip//langchain_groq",
        "@pip//langchain_huggingface",
        "@pip//python_dotenv",
        "@pip//sentence_transformers",
        "@pip//faiss_cpu",
    ],
)

py_binary(
    name = "spam_detector_simple",
    srcs = ["spam_detector_simple.py"],
    main = "spam_detector_simple.py",
    deps = [
        "@pip//langchain",
        "@pip//langchain_groq",
        "@pip//python_dotenv",
        "@pip//requests",
    ],
)