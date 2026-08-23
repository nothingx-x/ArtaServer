// a simple js script to be run in cloudflare
// for getting events of plugin

export default {
  async fetch(request, env, ctx) {
    const token = env.BOT_TOKEN;
    const chat_id = env.CHAT_ID;

    if (!token || !chat_id) {
      return new Response(
        "Configure BOT_TOKEN and CHAT_ID environment variables",
        { status: 400 },
      );
    }

    const url = new URL(request.url);
    if (url.pathname === "/webhook" && request.method === "POST") {
      try {
        const data = await request.json();
        const convertActionToPersian = (action) => {
          switch (action) {
            case "JOIN":
              return "وارد شد ✅";
            case "QUIT":
              return "خارج شد ❌";
            case "START":
              return "روشن شد 🚀";
            case "STOP":
              return "خاموش شد ⏹️";
            default:
              return action || "نامشخص";
          }
        };

        const action = data.action;
        const playername = data.playerName;

        let text = "";
        if (!playername) {
            text = convertActionToPersian(action)
        } else {
            text = `${playername} ${convertActionToPersian(action)}`;
        }
        try {

          const url = `https://api.telegram.org/bot${token}/sendMessage?chat_id=${chat_id}&text=${text}`;
          const result = await fetch(url);
          if (result.status != 200) {
            return new Response(`Error while sending to telegram ${result.statusText}`, {status: 400})
          }
        } catch(error) {
          return new Response("Error while sending to telegram", {status: 400})
        }

        return Response.json({
          status: "success",
        });
      } catch (error) {
        return new Response("Invalid JSON", { status: 400 });
      }
    }
    return new Response("Hello World!");
  },
};
