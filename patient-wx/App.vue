<script>
export default {
	onLaunch: function() {
		console.log('App Launch');

		
	},
	onShow: function() {
		console.log('App Show');
	},
	onHide: function() {
		console.log('App Hide');
	}
};
</script>

<style lang="scss">
@import 'uview-ui/index.scss';
@import "@/components/active-form/active-form.scss";//注入active-form样式
/*每个页面公共css */
</style>
