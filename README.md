# Indian-Sign-Language-Text-to-Speech-Translator

This project is a real-time Indian Sign Language (ISL) recognition system that converts hand gestures into text and speech, aiming to bridge the communication gap between individuals with hearing/speech impairments and the general public.

---

##  Features

-  Real-time hand gesture tracking using **MediaPipe**
-  Hand landmark recognition using a trained **MLP (Multi-Layer Perceptron)** model
-  Android app interface for real-time capture and display
-  Inference via lightweight **TensorFlow Lite (TFLite)** model
-  Text-to-Speech (TTS) output using Android’s TTS engine 
-  Flask backend exposed via **Cloudflare Tunnel**
-  Recognizes 26 alphabetic gestures (A–Z)
-  Words and sentences are dynamically formed from letter predictions
- Extend support to full-word and sentence-based ISL recognition

---

##  Model Architecture

- Input: 126 features (x, y, z coordinates of 21 landmarks per hand)
- Model: MLP with two hidden layers and dropout
- Accuracy: ~96% on test data
- Framework: TensorFlow/Keras
- Deployment: Converted to **TFLite** for mobile optimization

---

##  App Workflow

1. **User signs a gesture** → Camera captures frame
2. **MediaPipe extracts landmarks** → Sent to Flask server
3. **TFLite model predicts the letter**
4. **Letters form words** → Displayed on screen
5. **TTS Engine** converts text into speech
6. Optionally, **Swaram API** provides output in regional languages

---

##  Tech Stack

- **Frontend**: Android (Java/Kotlin)
- **Backend**: Flask (Python)
- **Modeling**: TensorFlow, Keras, TFLite
- **Hand Tracking**: MediaPipe
- **Speech Output**: Android TTS & Swaram API
- **Tunnel**: Cloudflare Tunnel
- **Data Storage**: CSV (hand landmarks)

---

##  Accuracy & Testing

- Achieved 96.07% accuracy on ISL alphabet classification.
- Tested under different lighting and hand positions.
- High robustness and low latency on mobile deployment.

---

##  Future Scope
- Integrate **temporal models** (e.g., LSTM, Transformer) for dynamic signs
- Improve gesture accuracy in challenging environments

---


