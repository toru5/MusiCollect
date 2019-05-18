# MusiCollect:
- Java is required to run this program. This can be obtained at: https://www.java.com/download/

## Installation:
- Download musiCollect.jar using this [link](https://github.com/toru5/MusiCollect/blob/master/MusiCollect.jar?raw=true)
- If you haven't installed Java, download the latest JRE using this <a href="https://www.java.com/download/">link</a>
- Run musiCollect.jar and have fun scraping!

## The overall program flow looks like this:
- Users can check off boxes corresponding to websites or services they would like to fetch music from
- Any number of checkboxes can be checked during a run
- There is a textfield next to each checkbox corresponding to the number of songs the user would like to fetch from that service
	- In some cases this textfield is used for username / password information
- Further to the right, there will sometimes be another textfield used for additional information. More on this in the breakdown of services
- There is a checkbox to the right of Exit button that will shuffle the order in which songs are added to the playlist
 this can be useful when selecting multiple websites, or if the user would like to mix up the order for whatever reason
- The user can choose to post the playlist to Spotify or YouTube by specifying this in the two toggle buttons underneath the console (defaults to spotify)
- Once the user is satisfied with input parameters, they will click the submit button, and detailed information about the fetching process will be posted in the console
- Once song data has been collected, the program will open the user's browser and request access to their Spotify or YouTube account (depending on what service is selected)
- If access is granted, the program will then add all the songs it can find (in the selected service) to a playlist under the user's account
- The program will finally open this new playlist in the user's default browser
	
## Breakdown of services:
- `Beatport:`
	- The program will fetch <X> songs from EACH genre selected from the list of beatport genres, where <X> is the number typed in the textfield to the right of the checkbox
	- Any number of genres can be selected
	- Maximum song limit is 100 (the program pulls from the top 100 list)
	- There is a checkbox at the bottom of the genre list that allows the user to pull random songs from the top 100 of each genre
- `Billboard:`
	- The program will fetch <X> songs from EACH genre selected from the list of billboard genres, where <X> is the number typed in the textfield to the right of the checkbox
	- Any number of genres can be selected
	- Maximum song limit depends on the genre. Generally this is between 40-50 (save for the case of the top 200), and the program will correctly throttle user requests based on unique limits
	- There is a checkbox at the bottom of the genre list that allows the user to pull random songs from each genre
- `Indie Shuffle:`
	- The program can fetch at most 15 songs from Indie Shuffle
- `Reddit:`
	- Any valid subreddit can be fetched from, this is accomplished by typing a valid subreddit id in the textfield below
	- The program will pull at MOST 100 listings, this is the limit set by reddit's API and cannot be changed right now
	- A minimum number of upvotes can be used as a filter, this is defaulted to 1, but can be changed by typing a number in the corresponding textfield
	- If the user selects subreddits with few musical posts, the program will have a hard time fetching valid results
	- For the best experience, select subreddits that are almost entirely musical posts
- `Last.fm Suggested Songs:`
	- This will fetch songs suggested to that user by the algorithms used in last.fm's services.  The limit on this is vague and it is recommended to keep it below 100
	- A valid last.fm username and password is required for this service.  The account must also be active enough to have a recommended songs page on the last.fm website
- `Last.fm Friends Top Songs:`
	- This will fetch top songs from all of the user's friends on last.fm.  It will equally divide the results between all friends, 
	such that the user receives music from every friend
	- If the user does not have enough friends to meet the quota, the program will terminate when it has exhausted the available lists, 
	it will still create the playlist with the available results
	- The user can change the time period over which to select from. This can be selected by the three toggle buttons to the right of the song quota
	- A valid last.fm username is required for this feature, but NOT a password. Moreover, any username can be specified, not just your own
- `Similar Music:`
	- This service will fetch music similar to, but not from, the specified artist
	- Multiple artists can be specified by separating them with semicolons (e.g. artist1; artist2; artist3; etc)
		- In this case, it will fetch <X> songs from EACH artist, where <X> is the specified song quota
	- It accomplishes this by fetching a large list of similar artists (determined by relationships in a large database built by last.fm) and picking them at random
	- Once a random artist has been picked, it will fetch a random song from their top 7 most popular songs
	- It repeats this process until the quota has been met. The program will keep track of unique choices such that no duplicate songs will be added

If you encounter any bugs, I would love to know, as I'm the sole contributor to this project, and I don't have a good environment for large-scale testing.
I would also love to hear any feedback you have, this could include features you would like to see, or just overall impression of the playlists, 
as parameters can be tightened or loosened to help create a better experience
Have fun scraping!
