<p align="center"><img src="https://user-images.githubusercontent.com/75981415/144453642-234ac244-d0dc-4574-a76b-ffe5bf3eb795.png" width ="300"></p>

# android5-boomerang
boomerang #미디어 #메모


## 프로젝트 소개

> 부메랑 어플리케이션은 미디어를 사용한 메모 작성 어플리케이션입니다.
동영상 메모 작성, 음성 메모 작성 방식이 있습니다.
동영상 메모는 나만의 메모, 모두의 메모 방식을 선택해서 작성할 수 있습니다.
음성 메모는 STT로 얻은 텍스트와 음성파일을 이용하여 싱크 텍스트 기능을 제공합니다.
>
## 조원 소개
| [K025 박태현](https://github.com/CrewDaniel)| [K057 최연두](https://github.com/Greenddoovie)|  [K060 최진형](https://github.com/JinhyungChoi)|
| -------- | -------- | -------- |
|<img src="https://user-images.githubusercontent.com/62787596/144439629-333a3e26-bd1e-4147-a169-227ceaaeb659.png" width="80"> |<img src="https://user-images.githubusercontent.com/62787596/144439613-30a47b82-ee32-48f5-957b-b70267dc108b.png" width="80">|<img src="https://user-images.githubusercontent.com/62787596/144439468-fd5dec51-b1c8-4c7b-a559-a2f08e8f1002.png" width="80">|

## 기능 소개
<img src="https://user-images.githubusercontent.com/62787596/144446454-8f51de98-154d-46f2-8142-0b95cfe0732a.gif" width=200>


### 홈화면
> 작성한 메모를 종류별, 시간별로 분류합니다.
>
> 메모의 제목을 검색할 수 있습니다. (Debounce 적용)
>
> StaggeredGridLayout으로 메모를 배치했습니다.
>

> 아이콘으로 메모의 종류를 구분할 수 있습니다.
>

### 모두의 메모
<img src="https://user-images.githubusercontent.com/62787596/144444812-317ae604-0621-4cb9-97dd-1049af66f2fe.png" width=200>

> 입력에 따라 동영상에 그림을 그릴 수 있습니다.
>
> 결과물은 MP4 파일로 인코딩되어 바로 재생, 공유할 수 있습니다.
>
> SurfaceTexture 클래스를 사용한 OpenGL ES 렌더링 과정은 [OpenGL ES를 사용한 동영상 렌더링](https://www.notion.so/OpenGL-ES-446fe6bd387d496eb7b58ff0dd45252d) 페이지에서 읽어보실 수 있습니다.
>

### 나만의 메모
<img src="https://user-images.githubusercontent.com/62787596/144445794-8e987c98-7684-4a9e-8506-cefd13a80ae2.png" width=200>

> 나만의 메모 화면에서는 여러 영상 메모를 남길 수 있습니다.
>
> 사용자는 특정 시간 대에 메모를 작성할 수 있고 펜의 색상을 바꿔가며 메모를 녹화할 수 있습니다.
>
> 메모를 완료하면 하단에 작성한 메모와 메모를 작성한 시간대가 표시됩니다.
>
> 해당 메모를 클릭하면 삭제 여부를 확인하고 삭제를 할 수 있습니다.
>

### 음성 메모
<img src="https://user-images.githubusercontent.com/62787596/144446697-0f82dafd-16c6-4a76-a6dd-ced10ff76681.gif" width=200><img src="https://user-images.githubusercontent.com/62787596/144446709-dcc79c38-c1d9-4c73-b153-c2c1598871b4.gif" width=200>


> 음성 메모 작성 화면에서는 STT를 이용하여 인식된 텍스트와 음성파일을 저장합니다.
>
> 음성 인식 버튼을 누르면 STT가 실행되고, STT는 사용자가 멈추기 전까지 계속 실행됩니다.
>
> 음성저장 버튼을 누르면 지금까지 말한 음성이 하나의 파일로 저장됩니다.
>
> 음성 메모 화면에서는 음성파일을 이용하여 메모를 확인합니다.
>
> 메모와 음성 싱크를 맞추어서 말하고 있는 부분이 강조되어 보여집니다.
>
> 듣고 싶은 부분의 메모를 눌러서 음성 진행 시간을 이동할 수 있습니다.
>
> 프로그레스바를 이동해서 음성파일의 시간을 변화시켜도 메모와 싱크됩니다.
>

## 기술 스택
- OpenGL ES
- Exoplayer
- MVVM Design Pattern
- STT
- Firebase Crashlytics
- Room
- Hilt

## 기술 특장점
### [OpenGL ES를 사용한 동영상 렌더링](https://www.notion.so/OpenGL-ES-446fe6bd387d496eb7b58ff0dd45252d)
### [PiP를 응용하여 동영상 동시 재생](https://www.notion.so/PiP-96972eda2cb5442f921fbb62f7b7412a)
### [STT Intent vs Speech Recognizer](https://www.notion.so/STT-Intent-vs-Speech-Recognizer-9bd941d0a5fb4de5ad7b2bf270a25dce)

## MAD ScoreCard


![image](https://user-images.githubusercontent.com/62787596/144438554-f7fc608e-5cd4-43ae-b21c-4f345bccf8d7.png)
![image](https://user-images.githubusercontent.com/62787596/144438654-37214f69-5e67-44eb-a6e2-1fdc1b5640ae.png)
![image](https://user-images.githubusercontent.com/62787596/144438680-35ebb637-34e0-40ac-947c-b0958d495d3f.png)

## 관련 링크

- [위키](https://github.com/boostcampwm-2021/android05-boomerang/wiki)
- [데모 영상](https://www.youtube.com/watch?v=OxBSFlZeykQ)
