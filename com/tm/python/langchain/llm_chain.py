from langchain_groq import ChatGroq
from langchain_core.prompts import ChatPromptTemplate

model = ChatGroq(model="llama-3.1-8b-instant")

system_template = "Translate the following from English into {language}"
prompt_template = ChatPromptTemplate.from_messages([
    ("system", system_template),
    ("user", "{text}")
])

chain = prompt_template | model

result = chain.invoke({"language": "Vietnamese", "text": "Hello, how are you?"})
print(result.content)