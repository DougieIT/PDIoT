import tensorflow as tf

def validate_tflite_model(file_path):
    try:
        interpreter = tf.lite.Interpreter(model_path=file_path)
        interpreter.allocate_tensors()
        print(f"{file_path} is a valid TFLite model.")
    except Exception as e:
        print(f"{file_path} is not valid. Error: {e}")

validate_tflite_model("model.tflite")
validate_tflite_model("social_signals.tflite")