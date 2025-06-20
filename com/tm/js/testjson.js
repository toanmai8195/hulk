const obj = {
    "roomId": "9223370410696535816fc2ac350-82c8-4c38-8085-3fbc04b393fb",
    "messageId": "00000001745400172946.0329",
    "messageSource": "P2P",
    "requestId": "85317702447",
    "messageType": "INFORMATIONAL_CARD",
    "campaignId": "transfer_money",
    "templateId": "",
    "text": "[Chuyển nhận tiền] cầu lông t7",
    "customData": {},
    "createdAt": 1745400172946,
    "updatedAt": 0,
    "sessionId": "",
    "isDelete": false,
    "variableValues": {},
    "trackingWebhook": "",
    "tracking": {},
    "senderProfile": {
        "profileId": "45972952",
        "senderName": "Khiếu Đức Toàn",
        "senderAvatar": "https://avatar.momocdn.net/avatar/d897/24a40468d04056a2e3da810ac8a9a8115d3413cbccead158350120e337d7.png"
    },
    "actorId": "45972952",
    "senderId": "45972952",
    "hideFrom": ["", ""],
    "mention": [],
    "templateData": {
        "items": {
            "default": {
                "order": 0,
                "actionId": "viewTransactionHistory",
                "body": {
                    "title": "Chuyển tiền",
                    "amount": "60000.0",
                    "status": "SUCCESS",
                    "content": {
                        "sender": "cầu lông t7",
                        "receiver": "cầu lông t7"
                    },
                    "stickerUrl": "",
                    "bottomImage": "",
                    "backgroundImage": {
                        "sender": "https://static.momocdn.net/app/img/p2p/transferred_background.png",
                        "receiver": "https://static.momocdn.net/app/img/p2p/received_background.png"
                    },
                    "transactionTimestamp": 1.745400172619E12
                },
                "footer": {}
            }
        },
        "p2p_info": {
            "amount": "60000.0",
            "status": "SUCCESS",
            "TID": "85317702447",
            "moneyRequestId": "",
            "serviceId": "transfer_p2p_search_paste"
        }
    },
    "actionData": {
        "viewTransactionHistory": {
            "actionType": "REDIRECT",
            "featureCode": "transaction_history_detail",
            "forwardingParams": {
                "tranId": "85317702447"
            }
        }
    },
    "oldType": "",
    "debug": ""
};

let totalStringify = 0;
let totalParse = 0;
let jsonString = "";

const startStr = performance.now();
for (let i = 0; i < 20; i++) {
    jsonString = JSON.stringify(obj);
}
const endStr = performance.now();
console.log(`JSON to string = ${(endStr - startStr).toFixed(6)} ms`);

const startParse = performance.now();
for (let i = 0; i < 20; i++) {
    JSON.parse(jsonString);
}
const endParse = performance.now();
console.log(`JSON to string = ${(endParse - startParse).toFixed(6)} ms`);