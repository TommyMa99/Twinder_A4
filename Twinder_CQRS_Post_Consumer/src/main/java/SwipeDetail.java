import java.io.Serializable;

public class SwipeDetail implements Serializable {
    private String swiper;
    private String swipee;
    private String comment;

    private String leftorright;

    public SwipeDetail(String swiper, String swipee, String comment) {
        this.swiper = swiper;
        this.swipee = swipee;
        this.comment = comment;
    }

    public String getSwiper() {
        return swiper;
    }

    public void setSwiper(String swiper) {
        this.swiper = swiper;
    }

    public String getLeftorright() {
        return this.leftorright;
    }

    public void setLeftorright(String action) {
        this.leftorright = action;
    }

    public String getSwipee() {
        return swipee;
    }

    public void setSwipee(String swipee) {
        this.swipee = swipee;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "SwipeDetail{" +
                "swiper='" + swiper + '\'' +
                ", swipee='" + swipee + '\'' +
                ", comment='" + comment + '\'' +
                ", leftorright='" + leftorright + '\'' +
                '}';
    }
}
