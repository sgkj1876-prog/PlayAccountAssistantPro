import express from "express";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import { GoogleGenAI } from "@google/genai";

const app = express();
app.use(helmet());
app.use(express.json({ limit: "20kb" }));
app.use("/api/", rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 20,
  standardHeaders: "draft-7",
  legacyHeaders: false
}));

const apiKey = process.env.GEMINI_API_KEY;
const modelName = process.env.GEMINI_MODEL || "gemini-flash-latest";
const port = Number(process.env.PORT || 8080);

if (!apiKey) {
  console.error("Missing GEMINI_API_KEY environment variable.");
  process.exit(1);
}

const ai = new GoogleGenAI({ apiKey });
const systemInstruction = `You are PlayAccount Assistant Pro, a safe bilingual assistant.
Help users understand official Google Account signup and Google Play setup.
Never ask for passwords, one-time codes, recovery codes, or payment secrets.
Never offer CAPTCHA bypass, phone verification bypass, bulk account creation,
fake identities, proxy rotation, or evasion of Google's limits.
Explain that account creation and verification must be completed by the user
on official Google pages. Answer in the requested language.
You are an independent assistant and are not Google support.`;

app.get("/health", (_req, res) => res.json({ ok: true, service: "PlayAccount Gemini API" }));

app.post("/api/chat", async (req, res) => {
  try {
    const message = typeof req.body?.message === "string" ? req.body.message.trim() : "";
    const language = req.body?.language === "en" ? "English" : "Arabic";
    if (!message || message.length > 4000) {
      return res.status(400).json({ error: "message_required_or_too_long" });
    }

    const result = await ai.models.generateContent({
      model: modelName,
      contents: `Reply in ${language}. User message: ${message}`,
      config: { systemInstruction, temperature: 0.4, maxOutputTokens: 900 }
    });

    return res.json({ reply: result.text || "No text response was returned." });
  } catch (error) {
    console.error("Gemini request failed:", error?.message || error);
    return res.status(502).json({ error: "gemini_request_failed" });
  }
});

app.listen(port, "0.0.0.0", () => console.log(`Gemini backend listening on ${port}`));
