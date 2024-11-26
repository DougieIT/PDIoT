import tensorflow as tf

try:
    interpreter = tf.lite.Interpreter(model_path="social_signals.tflite")
    interpreter.allocate_tensors()
    print("Model loaded successfully.")
except Exception as e:
    print(f"Error loading model: {e}")