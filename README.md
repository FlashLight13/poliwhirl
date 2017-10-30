[![Android Arsenal]( https://img.shields.io/badge/Android%20Arsenal-Poliwhirl-green.svg?style=flat )]( https://android-arsenal.com/details/1/6380 )
[![Maven Central]( https://img.shields.io/badge/Maven%20Central-available-brightgreen.svg?style=flat )](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22poliwhirl%22)
[![API](https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=14)

# Poliwhirl

<img src="https://cdn.bulbagarden.net/upload/thumb/a/a9/061Poliwhirl.png/250px-061Poliwhirl.png" width="150" height="150">

## Description

This is a small image processing library done to find good color for icon background. It uses [CIEDE2000](https://en.wikipedia.org/wiki/Color_difference#CIEDE2000) to  determine what colors can be processed as the same.

## Result example

Here goes the table with the generated colors.

Input | Result
------------ | -------------
<img src="https://github.com/FlashLight13/poliwhirl/blob/master/images/google_maps_input.png" width="100" height="100"> | <img src="https://github.com/FlashLight13/poliwhirl/blob/master/images/google_maps_result.png" width="100" height="100">
<img src="https://github.com/FlashLight13/poliwhirl/blob/master/images/habr_input.png" width="100" height="100"> | <img src="https://github.com/FlashLight13/poliwhirl/blob/master/images/habr_result.png" width="100" height="100">
<img src="https://github.com/FlashLight13/poliwhirl/blob/master/images/instagram_input.png" width="100" height="100"> | <img src="https://github.com/FlashLight13/poliwhirl/blob/master/images/instagram_result.png" width="100" height="100">

## Usage
Current stable version: 1.0.1

##### Gradle: 

`compile 'com.antonpotapov:poliwhirl:$version'`

##### Maven:

```
<dependency>
    <groupId>com.antonpotapov</groupId>
    <artifactId>poliwhirl</artifactId>
    <version>$version</version>
</dependency>
```

##### In code: 
You need to use only one class: `Poliwhirl()`. It's instance is reusable. So, you may create in only once. Also, there is no any long-time operations in instance creation, so you can create it each time you need it.

###### Async way
```
Poliwhirl().generateAsync(bitmap, object : Poliwhirl.Callback {
    override fun foundColor(color: Int) {
      // do whatever you need with the color
    }
})
```
###### Sync way
`Poliwhirl().generate(bitmap);`

###### Customizable way
Here you can provide an executor to poliwhirl. Poliwirl will try to execute it's calculation in parallel in this case. Also you can provide something like current-thread executor (thats how sync way is done) or whatever you need.
```
Poliwhirl().generateOnExecutor(bitmap, object : Poliwhirl.Callback {
    override fun foundColor(color: Int) {
        pictureBackground.setBackgroundColor(color)
    }
}, Executors.newSingleThreadExecutor())
```
