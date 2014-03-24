![CI status](https://secure.travis-ci.org/jeluard/renderist.png)

*This project is not maintained anymore.*

#Renderist

A webapp that turns textual representation of diagrams stored as gists into hosted images.

Find more details on http://renderist.herokuapp.com/.

## Deployment on heroku

As graphviz is used to generate some of the diagrams you will need to have its binaries installed on your deployment box.

On [heroku](http://heroku.com) this is done by executing following command:

```
heroku config:add BUILDPACK_URL=https://github.com/jeluard/heroku-buildpack-graphviz/ -a renderist
```

More details on https://github.com/jeluard/heroku-buildpack-graphviz.

Released under [GPLv3 license](http://www.gnu.org/licenses/gpl.html).
