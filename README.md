# LSTM based brain-machine interface tool for text generation through eyes blinking detection

### Autors

* [Andres Felipe Reyes](https://github.com/Andresreyesf)
* [Edgar Camilo Camacho](https://github.com/edgarcamilocamacho)
* Armando Mateus

### Abstract

This work presents the development of a tool that allows people to communicate, by using only their voluntary eyes blinks. This tool provides a communication link mainly for people with motor disabilities, who cannot communicate through voice. The electroencephalographic (EEG) signal provided by the Mindwave Mobile 2 headset is the source of information to detect the voluntary blinks. In order to perform the digital processing of the EEG signal, a Long-Short Term Memory (LSTM) neural network was implemented, for the classification of the EEG signal in one of 5 possible classes: No blink, One blink, Two blinks, Three blinks and Other. The results of the trained model achieved an average accuracy of 92%. The neural network was embedded in a native Android mobile application, that shows a virtual keyboard consisting of the 27 letters of the Spanish alphabet, plus three characters, that are the commands "delete", "space", and "enter". Each character can be selected by the user only through a determined number of voluntary blinks executed at certain times. When the user types a word and selects the "enter" command, the word is converted to audio using a text-to-speech engine. Finally, to verify the performance of the application, an experiment with eight people was executed, and as result the system achieved an average spelling precision of 91.3%.

### Dataset

### Training Notebook

### Android App

* Android SDK version: Android 10.0 (API level 29). The app also Works in previous versions from Android 6.0 (API level 23)
* buildTools version: 29.0.3
* Tensorflow-Android version: 1.13.1
* Neurosky SDK version: libStreamSDK 1.2.0


