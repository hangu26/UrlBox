## 🌐 **UrlBox**

웹페이지를 썸네일과 메모로 정리하는, 보기 쉽고 찾기 쉬운 **스마트 URL 북마크 앱**

---

### 📅 개발 기간

**2025.01.24 ~ 2025.03.01**

### 💡 프로젝트 개요

단순한 URL 저장에서 벗어나, 썸네일 이미지·태그·메모를 활용해 **가독성과 검색 편의성을 높인** 북마크 앱입니다.

일상에서 자주 URL을 저장하던 경험에서 출발해, 시각적이고 직관적인 정리를 가능하게 하려 기획했습니다.

---

### ✨ 주요 기능

- 📸 **URL 등록 시 자동 캡처 프레임 제공**
    
    → URL을 추가하면 해당 페이지로 즉시 이동 후 캡처 화면 등장. 스킵 가능
    
- 💾 **캡처 이미지 저장 및 상세 편집 기능**
    
    → 메인 화면에서 썸네일 클릭 시 URL 이름, 메모, 이미지 편집 가능
    
- 🏷️ **태그 분류 기능**
    
    → 저장 시 태그 지정 → 태그별 URL 목록 확인 가능
    
- 🔍 **태그 필터링 UI 구현**
    
    → 선택된 태그만 보여주고, ‘전체’ 선택 시 전체 URL 보기 가능
    
- 🖼️ **썸네일 전용 보기 탭**
    
    → 썸네일만 모아서 볼 수 있는 탭 제공
    

---

### 🧰 사용 기술

- **언어/패턴**: Kotlin, MVVM
- **Jetpack**: LiveData, ViewModel, DataBinding
- **UI/UX**: Glide, runOnUiThread, imageTransition
- **로컬 저장소**: Room
- **DI & 네트워크**: Koin, Gson, Callback

---

### 👨‍💻 담당 역할

**개인 프로젝트**로, 기획부터 디자인, 개발까지 **100% 혼자** 수행하였습니다.

---

### 🧠 트러블슈팅 및 구현 도전기

### 1️⃣ 태그 필터링 구현

### 📌 문제

- 어떤 태그가 선택되었는지 기억하고, 그에 따라 UI를 매끄럽게 갱신하는 로직이 어려웠음
- ‘전체’ 버튼을 누르면 모든 URL을 다시 보여줘야 했고선택이 바뀔 때마다 전체를 새로 고치면 성능이 떨어질 수 있었음

---

**✅ 해결 방법**

- 선택된 태그의 **인덱스만 저장**해서 이전/현재 항목만 `notifyItemChanged()`로 **최소 UI 갱신**
- `RecyclerView.Adapter`의 `ViewHolder` 안에서 클릭 이벤트 처리
- **‘전체’ 태그는 position == 0 으로 분기 처리**하여
    
    선택 해제 상태처럼 보이도록 특별히 구성
    

```kotlin

/** 
	notifyItemChanged(previousSelected)  
	notifyItemChanged(selectedPosition)  << 이전/현재 항목만 UI 갱신
	
if (selectedPosition == position) << 선택된 항목에 따라 배경 변경 
**/
inner class MyViewHolder(private val binding: ItemUrlTagBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(tag: Tag, position: Int) {

        if (position == 0) {
            binding.txTag.text = "전체"
        } else {
            binding.txTag.text = tag.tag
        }

        if (selectedPosition == position) {
            binding.btnTag.setBackgroundResource(R.drawable.border_url_tag_clicked)
        } else {
            binding.btnTag.setBackgroundResource(R.drawable.border_url_tag)
        }

        binding.btnTag.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = position

            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)

            if (position == 0) {
                
                filterListener.onTagFiltered(emptyList(), "전체")
            } else {
                tagList[selectedPosition].urlList?.let { urlList ->
                    filterListener.onTagFiltered(urlList, tag.tag.toString())
                }
            }
        }
    }
}

```

---

### 🎯 기대 효과

- 전체 리스트를 새로 고치지 않고, 필요한 항목만 갱신하여 **성능 최적화**
- 선택된 태그만 배경 색상 바뀌고, 클릭 시 UI가 부드럽게 전환되어 **UX 개선**
- ‘전체’ 태그도 **사용자 친화적 동작**으로 처리되어 일관된 필터링 경험 제공

### 2️⃣ 로딩 처리 개선

### 📌 문제

처음에는 URL과 태그 데이터를 Firebase에서 받아오는 동안 사용자에게 아무런 피드백이 없어, 앱이 멈춘 것처럼 보이는 문제가 있었습니다.

특히 로그인 상태, 게스트 상태에 따라 데이터를 다르게 처리해야 해서 **로딩 상태 제어가 복잡**했습니다.

**문제 상황**

- ViewModel에서 여러 데이터를 동시에 받아오면서 어떤 로딩 상태를 기준으로 UI를 제어할지 애매했음
- 예: URL 데이터가 로딩 중일 때는 로딩 바를 보여야 하지만, 태그 데이터도 동시에 받아오고 있으므로
    
    → 둘 중 하나만 끝나도 로딩 바가 사라지는 문제가 있었음
    

**해결 방법**

- `isLoading`과 `isTagLoading`이라는 두 개의 LiveData를 만들어 각각 URL과 태그 데이터의 로딩 상태를 따로 관리
- 둘 중 하나라도 `true`이면 로딩 UI 유지, 둘 다 `false`일 때만 실제 화면 노출
- 아래처럼 `observe()` 안에서 두 값을 함께 관찰하여 UI 상태를 동기화함

```kotlin

vm.isLoading.observe(viewLifecycleOwner) { isLoading ->
    vm.isTagLoading.observe(viewLifecycleOwner) { isTagLoading ->
        uBinding.loadingBarSkeleton.visibility = if (isLoading || isTagLoading) View.VISIBLE else View.GONE
        uBinding.skeletonLayout.visibility = if (isLoading || isTagLoading) View.VISIBLE else View.GONE
        uBinding.mainLayout.visibility = if (isLoading || isTagLoading) View.GONE else View.VISIBLE
    }
}
```

### 🎯 기대 효과

- 로딩 중에는 스켈레톤 UI와 로딩 바를 통해 사용자에게 **명확한 피드백** 제공
- 로딩 완료 후 자연스럽게 본 화면으로 전환되어 **UX가 개선**됨
- 자동 로그인 후 백업 데이터를 불러오는 경우에도 동일한 방식으로 적용하여
    
    → 앱 시작 시 **일관되고 깔끔한 로딩 흐름**을 구성할 수 있었음
    

## 3️⃣ Room vs Firebase 간 데이터 불일치 문제 및 동기화 흐름 개선

### 📌 문제

사용자가 URL을 저장할 때 다음과 같은 흐름으로 작동합니다:

- **Room에 먼저 저장** → 빠른 화면 반영 (UX 개선) → 이후 **Firebase에 저장**

그러나 이 방식은 다음과 같은 **문제**를 야기했습니다:

- 다른 기기에서 로그인 후 URL을 수정하거나 삭제한 경우
- 이전 기기에서는 여전히 **옛 데이터를 Room에서 불러오므로 불일치 발생**

결과적으로, 앱 재실행 시에도 **Firebase에서 변경된 최신 데이터가 반영되지 않음**

---

### ✅ 해결 방법: 데이터 동기화 흐름 개선

### 핵심 로직

앱을 **실행하고 첫 시작 시점**, 또는 **사용자가 명시적으로 새로고침 했을 때만** Firebase에서 전체 데이터를 가져옴

- 가져온 데이터를 **Room에 덮어쓰기 저장**
- 그 외 상황에서는 **Room의 데이터를 바로 표시**하여 빠른 로딩 속도 유지

```kotlin

if (isFirst == 1) {
    getUrlData(vm)
    getTagData(vm)
    pref.edit().putInt("isFirst", 0).commit()
    Log.e("모든 데이터 받아오기", "앱 시작 시 데이터 받아오기 성공")
}

```

---

### 🎯 기대 효과

- ✅ 여러 기기에서 로그인해도 **동기화된 최신 데이터 유지**
- ⚡ 일반 실행 시에는 Room 사용으로 **빠른 로딩 속도 확보**
- 📱 사용자 경험과 데이터 정합성 **동시에 만족**


---

### 📈 회고 및 성과

- **UI/UX 개선 경험**: 단순 저장이 아닌 **가독성과 검색 편의성** 중심으로 기능 구성
- **태그 필터링/전환 애니메이션** 등 다양한 UI 로직에 도전하며 **안드로이드 앱 완성도 향상**
- **혼자 기획부터 배포까지 경험**하며 전반적인 프로젝트 관리 능력 향상

---

### ✨ 느낀 점 & 성장한 점

이번 프로젝트를 진행하면서 **데이터 흐름과 사용자 경험(UX)**에 대해 깊이 고민하게 되었습니다.

특히 Firebase와 Room 간의 데이터 동기화 문제는 단순히 데이터를 불러오고 저장하는 것을 넘어서, **사용자 입장에서 앱이 어떻게 반응해야 자연스러운가?**를 중심으로 생각하는 계기가 되었습니다.

처음에는 URL과 태그 데이터를 가져오는 타이밍이 엇갈려 로딩 처리에서 많은 혼란이 있었고, 사용자에게 앱이 멈춘 것처럼 보이는 문제가 있었습니다. 이 경험을 통해 **상태를 명확히 나눠 관리하는 것이 얼마나 중요한지**, 그리고 **비동기 처리에서의 UI 대응이 사용자 신뢰와 직결된다는 사실**을 체감할 수 있었습니다.

또한, 태그 필터링 구현 과정에서는 단순한 클릭 이벤트 하나에도 **최소한의 UI 갱신, 선택 상태 기억, UX 일관성**까지 고려해야 한다는 점에서 디테일의 중요성을 배웠습니다. 선택 인덱스만 기억해서 이전 항목과 현재 항목만 갱신하도록 최적화하는 과정은 작은 성취감도 주었습니다.

결과적으로, 이번 프로젝트를 통해 단순한 기능 구현을 넘어서 **사용자 입장에서 앱이 "잘 작동한다"는 경험을 주는 것**이 개발자의 역할이라는 점을 더 깊이 이해하게 되었고,

앞으로도 이러한 고민을 담은 개발을 계속하고 싶다는 다짐을 할 수 있었습니다.