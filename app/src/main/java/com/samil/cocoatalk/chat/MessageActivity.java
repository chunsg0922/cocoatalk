package com.samil.cocoatalk.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.auth.User;
import com.samil.cocoatalk.R;
import com.samil.cocoatalk.model.ChatModel;
import com.samil.cocoatalk.model.UserModel;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MessageActivity extends AppCompatActivity {

    private String uid; // 자신 계정의 uid
    private String uid_other; // 상대방 계정의 uid
    private String chatRoom_uid; // 채팅방 uid
    private String profile_other;

    private Button button;
    private EditText edit;

    private RecyclerView recyclerView;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); // 로그인한 계정의 uid를 받아온다.
        uid_other = getIntent().getStringExtra("uid"); // 채팅 대상의 uid
        profile_other = getIntent().getStringExtra("profile"); // 채팅 대상의 profile
        button = (Button)findViewById(R.id.messageActivty_button);
        edit = (EditText)findViewById(R.id.messageActivity_editText);
        recyclerView = (RecyclerView)findViewById(R.id.messageActivity_recyclerView);

        // '전송' 버튼을 눌렀을 경우
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatModel chatModel = new ChatModel(); // 채팅방에 필요한 데이터를 정의한 ChatModel 클래스 객체 생성
                chatModel.users.put(uid, true);
                chatModel.users.put(uid_other, true);

                // 채팅방이 생성되어 있지 않은 경우
                if(chatRoom_uid == null){
                    button.setEnabled(false);
                    FirebaseDatabase.getInstance().getReference().child("chatRoom").push().setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {                    // 데이터베이스에 chatRoom이라는 문서에 채팅데이터를 추가해준다.
                        // push()를 통해 채팅방 식별이 가능하도록 value를 추가해준다.
                        @Override
                        public void onSuccess(Void unused) {
                            checkChatRoom();
                        }
                    });

                }
                // 채팅방이 생성되어 있는 경우
                else{
                    ChatModel.Comment comment = new ChatModel.Comment(); // ChatModel 하위 클래스로 생성한 comment의 객체를 생성하고,
                    comment.uid = uid;
                    comment.message = edit.getText().toString();
                    comment.timestamp = ServerValue.TIMESTAMP;
                    // 보내는 메시지를 chatRoom 테이블 아래에 comments라는 하위 테이블을 생성하여 데이터를 저장한다.
                    // DB에 데이터 전송이 완료되면 콜백 메소드인 onComplete를 수행한다. (입력칸 초기화)
                    FirebaseDatabase.getInstance().getReference().child("chatRoom").child(chatRoom_uid).child("comments").push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull @NotNull Task<Void> task) {
                            edit.setText("");
                        }
                    });


                }
            }
        });
        checkChatRoom();
    }

    // Firebase(DB)에 채팅방이 생성되어 있는지 확인하는 메소드
    void checkChatRoom(){
        FirebaseDatabase.getInstance().getReference().child("chatRoom").orderByChild("users/"+uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot dataSnapshot) {
                for(DataSnapshot item: dataSnapshot.getChildren()){
                    ChatModel chatModel = item.getValue(ChatModel.class); // DB에 해당 uid가 들어가있는지 확인하기 위해 chatModel 객체 생성
                    if(chatModel.users.containsKey(uid_other)){
                        chatRoom_uid = item.getKey(); // DB의 테이블 중 chatRoom에 생성된 데이터의 uid를 chatRoom_uid로 설정
                        button.setEnabled(true);
                        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
                        recyclerView.setAdapter(new RecyclerViewAdapter());
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }

    // 채팅 데이터를 출력하기 위한 Adapter 클래스
    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        List<ChatModel.Comment> comments; // ChatModel의 Comment 클래스 형태의 배열 선언
        UserModel userModel;
        public RecyclerViewAdapter(){
            comments = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference().child("users").child(uid_other).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull @NotNull DataSnapshot dataSnapshot) {
                    userModel = dataSnapshot.getValue(UserModel.class);
                    getMessageList();
                }

                @Override
                public void onCancelled(@NonNull @NotNull DatabaseError error) {

                }
            });

        }

        // 메시지 목록을 띄우는 메소드
        void getMessageList(){
            // chatRooms 테이블 중 해당하는 uid를 찾고, 그 하위 테이블의 데이터를 모두 불러온다.
            FirebaseDatabase.getInstance().getReference().child("chatRoom").child(chatRoom_uid).child("comments").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull @NotNull DataSnapshot dataSnapshot) {
                    comments.clear();
                    for(DataSnapshot item : dataSnapshot.getChildren()){
                        comments.add(item.getValue(ChatModel.Comment.class));
                    }
                    notifyDataSetChanged(); // 메시지 갱신(업데이트)
                    recyclerView.scrollToPosition(comments.size()-1); // 메소드가 실행될 때마다 리사이클러뷰의 스크롤을 최하단으로 설정해준다.
                }

                @Override
                public void onCancelled(@NonNull @NotNull DatabaseError error) {

                }
            });
        }

        @NonNull
        @NotNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
            MessageViewHolder messageViewHolder = ((MessageViewHolder) holder);

            // 채팅 데이터의 uid가 사용자일 경우
            if (comments.get(position).uid.equals(uid)) {
                messageViewHolder.textView_message.setText(comments.get(position).message); // 메시지의 텍스트를 표시한다.
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble);
                messageViewHolder.textView_name.setText("");
                messageViewHolder.linearLayout.setVisibility(View.VISIBLE);
                messageViewHolder.textView_message.setTextSize(20);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);
            }
            // 상대방이 메시지를 보내는 경우
            else{
//                if(profile_other.equals("")){
//                    messageViewHolder.imageView_profile.setImageResource(R.drawable.profile);
//                } else{
                    Glide.with(holder.itemView.getContext())
                            .load(userModel.getProfile())
                            .apply(new RequestOptions().circleCrop())
                            .into(messageViewHolder.imageView_profile); // 상대방의 프로필사진 출력
//                }
                messageViewHolder.textView_name.setText(userModel.getName()); // 상대방의 이름 출력
                messageViewHolder.linearLayout.setVisibility(View.VISIBLE);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setTextSize(20);
                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT);
            }

            long unixTime = (long)comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            messageViewHolder.textView_timestamp.setText(time);
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class MessageViewHolder extends RecyclerView.ViewHolder{
            public TextView textView_message;
            public TextView textView_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout;
            public LinearLayout linearLayout_main;
            public TextView textView_timestamp;

            public MessageViewHolder(View view){
                super(view);
                textView_message = (TextView)view.findViewById(R.id.messageItem_message);
                textView_name = (TextView)view.findViewById(R.id.messageItem_name);
                imageView_profile = (ImageView)view.findViewById(R.id.messageItem_profile);
                linearLayout = (LinearLayout)view.findViewById(R.id.messageItem_Linearlayout);
                linearLayout_main = (LinearLayout)view.findViewById(R.id.messageItem_Linearlayout_main);
                textView_timestamp = (TextView)view.findViewById(R.id.messageItem_timestamp);
            }
        }
    }
}