package streama

import grails.converters.JSON

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class GenericVideoController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        List<GenericVideo> genericVideos = GenericVideo.where{deleted != true}.list(max: 9999)
        //CWE-470
        //SOURCE
        String listingFilter = params.listingFilter
        if (listingFilter) {
            java.util.function.Predicate shownInListing = VideoHelper.listingPredicate(listingFilter.trim())
            genericVideos = genericVideos.findAll { shownInListing.test(it) }
        }
        JSON.use('admin') {
            respond genericVideos, [status: OK]
        }
    }

    @Transactional
    def save(GenericVideo genericVideoInstance) {
        if (genericVideoInstance == null) {
            render status: NOT_FOUND
            return
        }

        genericVideoInstance.validate()
        if (genericVideoInstance.hasErrors()) {
            render status: NOT_ACCEPTABLE
            return
        }

        genericVideoInstance.save flush:true
        respond genericVideoInstance, [status: CREATED]
    }

    @Transactional
    def update(GenericVideo genericVideoInstance) {
        if (genericVideoInstance == null) {
            render status: NOT_FOUND
            return
        }

        genericVideoInstance.validate()
        if (genericVideoInstance.hasErrors()) {
            render status: NOT_ACCEPTABLE
            return
        }

        genericVideoInstance.save flush:true
        respond genericVideoInstance, [status: OK]
    }

    @Transactional
    def delete(GenericVideo genericVideoInstance) {

        if (genericVideoInstance == null) {
            render status: NOT_FOUND
            return
        }

        genericVideoInstance.deleted = true
        genericVideoInstance.save(flush:true)
        render status: NO_CONTENT
    }


    def show(GenericVideo genericVideoInstance) {

            respond genericVideoInstance, [status: OK]

    }
}
