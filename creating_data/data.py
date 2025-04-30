import cv2
import mediapipe as mp
import pandas as pd
import time
import os

# Initialize MediaPipe Hands
mp_hands = mp.solutions.hands
hands = mp_hands.Hands(static_image_mode=False, max_num_hands=2, min_detection_confidence=0.7)
mp_drawing = mp.solutions.drawing_utils

# Create folder for data if not exists
if not os.path.exists("sign_data"):
    os.makedirs("sign_data")

# Get the label from user
label = input("Enter the letter you're signing (A-Z): ").upper()
if not label.isalpha() or len(label) != 1:
    print("Invalid input. Please enter a single alphabet letter.")
    exit()

# Data storage
landmark_data = []
total_samples = 50
captured_samples = 0
last_capture_time = 0

# Start webcam
cap = cv2.VideoCapture(0)
print("Starting webcam...")

while cap.isOpened() and captured_samples < total_samples:
    ret, frame = cap.read()
    if not ret:
        break

    frame = cv2.flip(frame, 1)
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = hands.process(rgb)

    h, w, _ = frame.shape
    landmarks = []

    # Extract landmarks for both hands
    if results.multi_hand_landmarks:
        hands_present = len(results.multi_hand_landmarks)
        for hand_landmarks in results.multi_hand_landmarks[:2]:
            for lm in hand_landmarks.landmark:
                landmarks.extend([lm.x, lm.y, lm.z])
        # Pad with zeros if only one hand detected
        if hands_present == 1:
            landmarks.extend([0.0] * (21 * 3))
    else:
        # No hands detected → All zeros
        landmarks = [0.0] * (42 * 3)

    # Countdown and save logic
    time_elapsed = time.time() - last_capture_time
    countdown = int(3 - time_elapsed)
    if time_elapsed >= 3 and landmarks.count(0.0) < 63:  # Ensure at least one hand is detected
        landmark_data.append([label] + landmarks)
        captured_samples += 1
        last_capture_time = time.time()

    # Draw landmarks
    if results.multi_hand_landmarks:
        for hand_landmarks in results.multi_hand_landmarks:
            mp_drawing.draw_landmarks(frame, hand_landmarks, mp_hands.HAND_CONNECTIONS)

    # Overlay text
    cv2.putText(frame, f"Letter: {label}", (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 0), 2)
    cv2.putText(frame, f"Captured: {captured_samples}/{total_samples}", (10, 70), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)
    if countdown > 0:
        cv2.putText(frame, f"Next in: {countdown}", (10, 110), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
    else:
        cv2.putText(frame, "Capturing...", (10, 110), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 128, 255), 2)

    if landmarks.count(0.0) == 63:
        cv2.putText(frame, "No hand detected!", (10, 150), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)

    cv2.imshow("Sign Language Data Collection", frame)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# Cleanup
cap.release()
cv2.destroyAllWindows()

# Save to CSV
save_dir = r"E:\project\creating_data\data"
os.makedirs(save_dir, exist_ok=True)

# Build full file path
save_path = f"{save_dir}/{label}_sign_data.csv"

# Save to CSV
df = pd.DataFrame(landmark_data)
df.to_csv(save_path, index=False, header=False)
print(f"Saved {captured_samples} samples for letter '{label}' to {save_path}")
