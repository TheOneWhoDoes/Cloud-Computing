import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;
import java.time.LocalTime;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TweetPerCountry {

    // list of countries
    public static String[] states = {"New York", "Texas", "California", "Florida"};

    public static class TPCMapper extends Mapper<Object, Text, Text, IntWritable> {

        // output variables
        private Text state = new Text("");
        private IntWritable out_value = new IntWritable();

        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {

            // splitting the input line and storing tweet and country fields
            String[] fields = value.toString().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            String tweet = fields[2].toLowerCase();
            String state_field = fields[18];
			String created_at = fields[0];
            
            // finding user's country
            for (String c : states) {
                if (state_field.contains(c)) {
                    state.set(c);
                    break;
                }
            }

            if (state.toString().equals(""))
                return;
			
			
			try {
			DateFormat inputFormat  = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a");
			DateFormat outputFormat = new SimpleDateFormat("HH:mm:ss");
			String s11 = outputFormat.format(inputFormat.parse(created_at));
		
			boolean isbefore = LocalTime.parse(s11).isBefore(LocalTime.parse("17:00:00"));
			boolean isAfter  = LocalTime.parse(s11).isAfter(LocalTime.parse("09:00:00"));
			if (!isbefore || !isAfter)
                return;
			}
			catch (Exception e) {
				//The handling for the code
			}
			

            // looking for Biden hashtags
            Boolean has_biden = false;
            if (tweet.contains("#biden") ||
                tweet.contains("#joebiden"))
                has_biden = true;

            // looking for Trump hashtags
            Boolean has_trump = false;
            if (tweet.contains("#trump") ||
                tweet.contains("#donaldtrump"))
                has_trump = true;

            // writing (key, value) into the context
            // key: country name
            // value: 0 -> only Biden, 1 -> only Trump, 2 -> both
            if (has_biden && !has_trump)
                out_value.set(0);
            else if (!has_biden && has_trump)
                out_value.set(1);
            else if (has_biden && has_trump)
                out_value.set(2);
            else
                return;

            context.write(state, out_value);
        }
    }

    public static class TPCReducer extends Reducer<Text, IntWritable, Text, Text> {
        
        private Text result = new Text();
        
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {

            int bidenCount = 0;
            int trumpCount = 0;
            int bothCount = 0;
            int numTweets = 0;

            // calculating the number of tweets related to each candidate
            for (IntWritable val : values) {
                numTweets += 1;

                if (val.get() == 0)
                    bidenCount += 1;
                else if (val.get() == 1)
                    trumpCount += 1;
                else
                    bothCount += 1;
            }

            // removing duplicate counts
            bothCount /= 2;
            numTweets -= bothCount;

            // calculating percentages
            float bothPercent = (float) bothCount / numTweets;
            float bidenPercent = (float) bidenCount / numTweets;
            float trumpPercent = (float) trumpCount / numTweets;

            // wrting into the context
            result.set(bothPercent + " " + bidenPercent + " " + trumpPercent + " " + numTweets);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {

        // job configuration
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Tweet per Countary");
        job.setJarByClass(TweetPerCountry.class);
        job.setMapperClass(TPCMapper.class);
        job.setReducerClass(TPCReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        // specifying input and output directories
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // executing the job
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}